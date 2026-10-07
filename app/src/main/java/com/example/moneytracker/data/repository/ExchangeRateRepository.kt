package com.example.moneytracker.data.repository

import android.content.Context
import com.example.moneytracker.data.model.ExchangeRates
import com.example.moneytracker.ui.transaction.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ExchangeRateRepository(context: Context) {
    private val preferences = context.getSharedPreferences("exchange_rates", Context.MODE_PRIVATE)

    fun cached(): ExchangeRates? = preferences.getString("response", null)?.let { raw ->
        runCatching { parse(raw) }.getOrNull()
    }

    suspend fun load(refresh: Boolean): ExchangeRates = withContext(Dispatchers.IO) {
        val cache = cached()
        val now = System.currentTimeMillis()
        val fetchedAt = preferences.getLong("fetchedAt", 0)
        // Rates update daily; avoid repeated requests when users tap Refresh.
        if (cache != null && ((!refresh && now < cache.nextUpdateMillis) || now - fetchedAt < 3_600_000)) {
            return@withContext cache
        }
        val connection = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == 200) { "Rates are temporarily unavailable" }
            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            val rates = parse(raw)
            preferences.edit().putString("response", raw).putLong("fetchedAt", now).apply()
            rates
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(raw: String): ExchangeRates {
        val response = JSONObject(raw)
        check(response.getString("result") == "success" && response.getString("base_code") == "USD")
        val values = response.getJSONObject("rates")
        val rates = Money.currencies.associateWith { code ->
            values.getString(code).toBigDecimal().also { require(it.signum() > 0) }
        }
        val updated = response.getLong("time_last_update_unix") * 1000
        val next = response.getLong("time_next_update_unix") * 1000
        require(updated > 0 && next > updated)
        return ExchangeRates(updated, next, rates)
    }
}

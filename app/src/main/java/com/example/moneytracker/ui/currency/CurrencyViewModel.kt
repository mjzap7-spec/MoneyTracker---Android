package com.example.moneytracker.ui.currency

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.model.ExchangeRates
import com.example.moneytracker.data.repository.ExchangeRateRepository
import com.example.moneytracker.ui.transaction.Money
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CurrencyUiState(
    val rates: ExchangeRates? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val from: String = "USD",
    val to: String = "VND",
    val amount: String = "1"
)

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExchangeRateRepository(application)
    private val state = MutableStateFlow(CurrencyUiState(rates = repository.cached()))
    val uiState = state.asStateFlow()
    private var initialized = false

    fun initialize(from: String) {
        if (initialized) return
        initialized = true
        state.value = state.value.copy(from = from.takeIf { it in Money.currencies } ?: "USD",
            to = if (from == "VND") "USD" else "VND")
        refresh(false)
    }

    fun selectFrom(code: String) { state.value = state.value.copy(from = code) }
    fun selectTo(code: String) { state.value = state.value.copy(to = code) }
    fun setAmount(amount: String) { state.value = state.value.copy(amount = amount) }
    fun swap() { state.value = state.value.copy(from = state.value.to, to = state.value.from) }

    fun refresh(force: Boolean = true) {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val rates = repository.load(force)
                state.value = state.value.copy(rates = rates, loading = false, error = null)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                state.value = state.value.copy(loading = false,
                    error = "Couldn't update rates. Check your connection and try again.")
            }
        }
    }
}

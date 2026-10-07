package com.example.moneytracker.ui.transaction

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.util.Currency

object Money {
    val currencies = listOf("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CNY", "INR", "VND", "CHF")

    fun parseCents(input: String, currencyCode: String = "USD"): Long? = try {
        val value = input.trim()
        val decimals = Currency.getInstance(currencyCode).defaultFractionDigits
        val pattern = if (decimals == 0) "[0-9]+" else "[0-9]+(\\.[0-9]{1,2})?"
        if (!value.matches(Regex(pattern))) null
        else BigDecimal(value).movePointRight(2).longValueExact().takeIf { it > 0 }
    } catch (_: ArithmeticException) {
        null
    } catch (_: NumberFormatException) {
        null
    }

    fun format(cents: Long, currencyCode: String = "USD"): String {
        return formatAmount(BigDecimal.valueOf(cents, 2), currencyCode)
    }

    fun formatAmount(amount: BigDecimal, currencyCode: String): String {
        val currency = Currency.getInstance(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            this.currency = currency
            minimumFractionDigits = currency.defaultFractionDigits
            maximumFractionDigits = currency.defaultFractionDigits
        }
        return "$currencyCode ${formatter.format(amount)}"
    }

    fun editable(cents: Long): String = BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()
}

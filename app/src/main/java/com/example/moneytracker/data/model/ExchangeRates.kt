package com.example.moneytracker.data.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Currency

data class ExchangeRates(
    val updatedAtMillis: Long,
    val nextUpdateMillis: Long,
    val rates: Map<String, BigDecimal>
) {
    fun rate(from: String, to: String): BigDecimal {
        val source = requireNotNull(rates[from]) { "Currency rate unavailable" }
        val target = requireNotNull(rates[to]) { "Currency rate unavailable" }
        require(source > BigDecimal.ZERO && target > BigDecimal.ZERO)
        return target.divide(source, MathContext.DECIMAL128)
    }

    fun convert(amount: BigDecimal, from: String, to: String): BigDecimal {
        require(amount >= BigDecimal.ZERO)
        return amount.multiply(rate(from, to), MathContext.DECIMAL128)
            .setScale(Currency.getInstance(to).defaultFractionDigits, RoundingMode.HALF_UP)
    }
}

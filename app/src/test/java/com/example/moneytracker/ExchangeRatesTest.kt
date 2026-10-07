package com.example.moneytracker

import com.example.moneytracker.data.model.ExchangeRates
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ExchangeRatesTest {
    // Fixed sample rates for deterministic calculations; not market data.
    private val rates = ExchangeRates(1000, 2000, mapOf(
        "USD" to BigDecimal.ONE, "EUR" to BigDecimal("0.8"),
        "VND" to BigDecimal("25000"), "JPY" to BigDecimal("150.555")
    ))

    @Test fun convertsAcrossBothCurrenciesUsingTheirRatio() {
        assertEquals(BigDecimal("3125000"), rates.convert(BigDecimal("100"), "EUR", "VND"))
        assertEquals(BigDecimal("80.00"), rates.convert(BigDecimal("2500000"), "VND", "EUR"))
        assertEquals(BigDecimal("100.00"), rates.convert(BigDecimal("100"), "USD", "USD"))
    }

    @Test fun roundsAccordingToDestinationCurrency() {
        assertEquals(BigDecimal("151"), rates.convert(BigDecimal.ONE, "USD", "JPY"))
        assertEquals(BigDecimal("0.80"), rates.convert(BigDecimal("1.005"), "USD", "EUR"))
        assertEquals(BigDecimal("0"), rates.convert(BigDecimal.ZERO, "USD", "JPY"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnavailableCurrency() { rates.rate("USD", "XXX") }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeAmount() { rates.convert(BigDecimal("-1"), "USD", "EUR") }
}

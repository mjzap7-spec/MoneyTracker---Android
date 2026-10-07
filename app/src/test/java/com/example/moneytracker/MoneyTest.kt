package com.example.moneytracker

import com.example.moneytracker.ui.transaction.Money
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun currencyControlsPrecisionAndDisplay() {
        assertEquals(1200L, Money.parseCents("12", "JPY"))
        assertNull(Money.parseCents("12.50", "JPY"))
        assertEquals(1250L, Money.parseCents("12.50", "EUR"))
        assertTrue(Money.format(1250, "EUR").startsWith("EUR "))
        assertTrue(Money.format(1200, "JPY").startsWith("JPY "))
        assertFalse(Money.format(1200, "JPY").contains(".00"))
        assertEquals("12", Money.editable(1200))
    }
    @Test fun amountsPreserveEveryCent() {
        assertEquals(29L, Money.parseCents("0.29"))
        assertEquals(1999L, Money.parseCents("19.99"))
        assertEquals(1000L, Money.parseCents(" 10 "))
        assertEquals(Long.MAX_VALUE, Money.parseCents("92233720368547758.07"))
        assertEquals("19.99", Money.editable(1999))
    }

    @Test fun invalidAmountsCannotBeSaved() {
        listOf("", "0", "-1", "NaN", "Infinity", "1.999", "1e3",
            "92233720368547758.08").forEach { assertNull(it, Money.parseCents(it)) }
    }
}

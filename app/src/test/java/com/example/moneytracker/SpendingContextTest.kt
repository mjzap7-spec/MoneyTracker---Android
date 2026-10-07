package com.example.moneytracker

import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.ui.assistant.SpendingContext
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Calendar

class SpendingContextTest {
    @Test fun summaryExcludesNotesAndOtherCurrencies() {
        val month = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 1) }
        val records = listOf(
            Transaction(amountCents = 1500, categoryId = "Food", description = "private-note-123", transactionDate = month.timeInMillis),
            Transaction(amountCents = 9900, currencyCode = "EUR", categoryId = "Secret EUR category", transactionDate = month.timeInMillis))
        val result = SpendingContext.build(records, month, "USD", 2000)
        assertTrue(result.contains("USD $15.00"))
        assertTrue(result.contains("remaining: USD $5.00"))
        assertFalse(result.contains("private-note-123"))
        assertFalse(result.contains("Secret EUR category"))
        assertFalse(result.contains("99.00"))
    }
}

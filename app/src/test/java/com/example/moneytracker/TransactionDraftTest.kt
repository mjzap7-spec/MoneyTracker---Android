package com.example.moneytracker

import com.example.moneytracker.ui.assistant.TransactionDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDraftTest {
    private fun draft(amount: String = "15.25", currency: String = "USD", type: String = "expense",
        category: String = "Food", date: String = "2026-10-05") = TransactionDraft.validated(
        amount, currency, type, category, "Lunch", date, "Food category inferred.")
    @Test fun draftPreservesExactMoneyAndEditableFields() {
        val result = draft()
        assertEquals(1525L, result.amountCents)
        assertEquals("Food", result.category)
        assertEquals("Lunch", result.note)
        assertEquals("2026-10-05", result.date.toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidCalendarDate() { draft(date = "2026-02-29") }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedCurrency() { draft(currency = "KRW") }
    @Test(expected = IllegalArgumentException::class) fun rejectsFractionalWholeUnitCurrency() { draft(currency = "JPY") }
    @Test(expected = IllegalArgumentException::class) fun rejectsZeroAmount() { draft(amount = "0") }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownType() { draft(type = "transfer") }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownCategory() { draft(category = "Unrecognized") }
    @Test(expected = IllegalArgumentException::class) fun rejectsAmountOverflow() { draft(amount = "999999999999999999999") }
}

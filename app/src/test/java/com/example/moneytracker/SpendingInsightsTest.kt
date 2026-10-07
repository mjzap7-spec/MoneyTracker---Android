package com.example.moneytracker

import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.ui.insights.SpendingInsights
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SpendingInsightsTest {
    @Test fun spendingChangeHandlesZeroAndBothDirections() {
        assertEquals(null, SpendingInsights.changePercent(100, 0))
        assertEquals(50, SpendingInsights.changePercent(1500, 1000))
        assertEquals(-25, SpendingInsights.changePercent(750, 1000))
        assertEquals(0, SpendingInsights.changePercent(1000, 1000))
    }
    private fun date(year: Int, month: Int) = Calendar.getInstance().apply {
        clear(); set(year, month, 15)
    }
    @Test fun monthlyTotalsKeepYearsAndCurrenciesSeparate() {
        val month = date(2026, Calendar.OCTOBER)
        val transactions = listOf(
            Transaction(type = "expense", amountCents = 1200, categoryId = "Food", transactionDate = month.timeInMillis),
            Transaction(type = "expense", amountCents = 800, categoryId = "Food", transactionDate = month.timeInMillis),
            Transaction(type = "income", amountCents = 9000, transactionDate = month.timeInMillis),
            Transaction(type = "expense", amountCents = 5000, currencyCode = "EUR", transactionDate = month.timeInMillis),
            Transaction(type = "expense", amountCents = 7000, transactionDate = date(2025, Calendar.OCTOBER).timeInMillis),
            Transaction(type = "expense", amountCents = 7000, transactionDate = date(2026, Calendar.SEPTEMBER).timeInMillis))
        val summary = SpendingInsights.calculate(transactions, month, "USD")
        assertEquals(9000L, summary.income)
        assertEquals(2000L, summary.expense)
        assertEquals(listOf("Food" to 2000L), summary.categories)
    }
    @Test fun budgetProgressHandlesMissingAndExceededLimits() {
        assertEquals(0, SpendingInsights.budgetPercent(200, 0))
        assertEquals(25, SpendingInsights.budgetPercent(250, 1000))
        assertEquals(100, SpendingInsights.budgetPercent(2000, 1000))
    }
}

package com.example.moneytracker

import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.ui.transaction.TransactionFilters
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TransactionFiltersTest {
    private fun day(year: Int, month: Int, day: Int, hour: Int = 0) = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0)
    }.timeInMillis

    private val transactions = listOf(
        Transaction(id = "lunch", amountCents = 1250, categoryId = "Food", description = "Lunch",
            transactionDate = day(2026, Calendar.OCTOBER, 6, 12)),
        Transaction(id = "salary", type = "income", amountCents = 100000, categoryId = "Salary",
            currencyCode = "EUR", transactionDate = day(2026, Calendar.OCTOBER, 5)),
        Transaction(id = "coffee", amountCents = 400, categoryId = "Food", description = "Coffee",
            currencyCode = "EUR", transactionDate = day(2026, Calendar.OCTOBER, 6, 23)),
        Transaction(id = "previous-year", categoryId = "Food", transactionDate = day(2025, Calendar.OCTOBER, 6))
    )

    @Test fun historyStartsWithEveryDateAndCurrency() {
        assertEquals(transactions, TransactionFilters.apply(transactions))
    }

    @Test fun selectedDayIncludesWholeDayAndExcludesOtherYears() {
        assertEquals(listOf("lunch", "coffee"), TransactionFilters.apply(transactions,
            selectedDateMillis = day(2026, Calendar.OCTOBER, 6)).map { it.id })
    }

    @Test fun filtersCombineAndSearchIgnoresCaseAndWhitespace() {
        assertEquals(listOf("coffee"), TransactionFilters.apply(transactions, type = "expense",
            currencyCode = "EUR", query = "  COFFEE ").map { it.id })
        assertEquals(emptyList<Transaction>(), TransactionFilters.apply(transactions, type = "income", query = "Food"))
        assertEquals(listOf("salary"), TransactionFilters.apply(transactions, type = "income").map { it.id })
    }

    @Test fun monthAndCategoryFiltersExcludeOtherYearsAndCombineWithCurrency() {
        assertEquals(listOf("coffee"), TransactionFilters.apply(transactions, category = "Food", currencyCode = "EUR",
            monthMillis = day(2026, Calendar.OCTOBER, 1)).map { it.id })
        assertEquals(emptyList<Transaction>(), TransactionFilters.apply(transactions,
            monthMillis = day(2026, Calendar.SEPTEMBER, 1)))
        assertEquals(listOf("previous-year"), TransactionFilters.apply(transactions, category = "Food",
            monthMillis = day(2025, Calendar.OCTOBER, 1)).map { it.id })
    }

    @Test fun filteredTotalsKeepCurrenciesAndTypesSeparate() {
        val totals = TransactionFilters.totals(TransactionFilters.apply(transactions,
            monthMillis = day(2026, Calendar.OCTOBER, 1)))
        assertEquals(listOf("EUR", "USD"), totals.map { it.currency })
        assertEquals(100000L, totals[0].income)
        assertEquals(400L, totals[0].expense)
        assertEquals(0L, totals[1].income)
        assertEquals(1250L, totals[1].expense)
        assertEquals(emptyList<com.example.moneytracker.ui.transaction.CurrencyTotals>(), TransactionFilters.totals(emptyList()))
    }
}

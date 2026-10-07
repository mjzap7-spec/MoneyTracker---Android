package com.example.moneytracker

import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.ui.transaction.TransactionCsv
import com.example.moneytracker.ui.transaction.TransactionFilters
import org.junit.Assert.*
import org.junit.Test

class TransactionCsvTest {
    @Test fun exportKeepsCurrencyAmountsAndEscapesNotes() {
        val csv = TransactionCsv.export(listOf(Transaction(amountCents = 1234, currencyCode = "EUR",
            categoryId = "Food", description = "Lunch, \"friends\"\nCafe")))
        assertTrue(csv.startsWith("Date,Type,Amount,Currency,Category,Note\r\n"))
        assertTrue(csv.contains("\"12.34\",\"EUR\""))
        assertTrue(csv.contains("\"Lunch, \"\"friends\"\"\nCafe\""))
    }

    @Test fun spreadsheetFormulaNotesAreExportedAsText() {
        val csv = TransactionCsv.export(listOf(Transaction(description = "=1+1")))
        assertTrue(csv.contains("\"'=1+1\""))
    }

    @Test fun sortingRespectsCurrencyGroupsAndDates() {
        val items = listOf(
            Transaction(id = "usd-low", amountCents = 10, transactionDate = 30),
            Transaction(id = "eur", currencyCode = "EUR", amountCents = 100, transactionDate = 10),
            Transaction(id = "usd-high", amountCents = 200, transactionDate = 20)
        )
        assertEquals(listOf("eur", "usd-high", "usd-low"), TransactionFilters.sort(items, "highest").map { it.id })
        assertEquals(listOf("eur", "usd-low", "usd-high"), TransactionFilters.sort(items, "lowest").map { it.id })
        assertEquals(listOf("eur", "usd-high", "usd-low"), TransactionFilters.sort(items, "oldest").map { it.id })
    }
}

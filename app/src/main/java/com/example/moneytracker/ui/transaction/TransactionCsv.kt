package com.example.moneytracker.ui.transaction

import com.example.moneytracker.data.model.Transaction
import java.time.Instant
import java.time.ZoneId

object TransactionCsv {
    fun export(transactions: List<Transaction>): String {
        fun cell(value: String): String {
            // Keep notes beginning with spreadsheet formulas as plain text.
            val safe = if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
            return "\"${safe.replace("\"", "\"\"")}\""
        }
        return buildString {
            append("Date,Type,Amount,Currency,Category,Note\r\n")
            transactions.forEach { transaction ->
                val date = Instant.ofEpochMilli(transaction.transactionDate).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                append(listOf(date, transaction.type, Money.editable(transaction.amountCents),
                    transaction.currencyCode, transaction.categoryId, transaction.description)
                    .joinToString(",") { cell(it) })
                append("\r\n")
            }
        }
    }
}

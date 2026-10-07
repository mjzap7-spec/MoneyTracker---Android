package com.example.moneytracker.ui.transaction

import com.example.moneytracker.data.model.Transaction
import java.util.Calendar

object TransactionFilters {
    val sortOrders = listOf("newest", "oldest", "highest", "lowest")

    fun sort(transactions: List<Transaction>, order: String): List<Transaction> = when (order) {
        "oldest" -> transactions.sortedBy { it.transactionDate }
        "highest" -> transactions.sortedWith(compareBy<Transaction> { it.currencyCode }.thenByDescending { it.amountCents })
        "lowest" -> transactions.sortedWith(compareBy<Transaction> { it.currencyCode }.thenBy { it.amountCents })
        else -> transactions.sortedByDescending { it.transactionDate }
    }
    fun apply(
        transactions: List<Transaction>,
        selectedDateMillis: Long? = null,
        type: String = "all",
        currencyCode: String? = null,
        query: String = "",
        category: String? = null,
        monthMillis: Long? = null
    ): List<Transaction> {
        val date = selectedDateMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
        val search = query.trim()
        val month = monthMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
        return transactions.filter { transaction ->
            val matchesDate = date == null || Calendar.getInstance().apply {
                timeInMillis = transaction.transactionDate
            }.let {
                it.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                    it.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
            }
            val matchesMonth = month == null || Calendar.getInstance().apply {
                timeInMillis = transaction.transactionDate
            }.let { it.get(Calendar.YEAR) == month.get(Calendar.YEAR) && it.get(Calendar.MONTH) == month.get(Calendar.MONTH) }
            matchesDate && matchesMonth && (category == null || transaction.categoryId == category) &&
                (type == "all" || transaction.type == type) &&
                (currencyCode == null || transaction.currencyCode == currencyCode) &&
                (search.isBlank() || transaction.categoryId.contains(search, ignoreCase = true) ||
                    transaction.description.contains(search, ignoreCase = true))
        }
    }
    fun totals(transactions: List<Transaction>): List<CurrencyTotals> = transactions.groupBy { it.currencyCode }
        .toSortedMap().map { (code, rows) -> CurrencyTotals(code,
            rows.filter { it.type == "income" }.sumOf { it.amountCents },
            rows.filter { it.type == "expense" }.sumOf { it.amountCents }) }
}

data class CurrencyTotals(val currency: String, val income: Long, val expense: Long)

package com.example.moneytracker.ui.insights

import com.example.moneytracker.data.model.Transaction
import java.util.Calendar

data class MonthlySpending(val income: Long, val expense: Long, val categories: List<Pair<String, Long>>)

object SpendingInsights {
    fun calculate(transactions: List<Transaction>, month: Calendar, currency: String): MonthlySpending {
        val matching = transactions.filter {
            val date = Calendar.getInstance().apply { timeInMillis = it.transactionDate }
            it.currencyCode == currency && date.get(Calendar.YEAR) == month.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == month.get(Calendar.MONTH)
        }
        val expenses = matching.filter { it.type == "expense" }
        return MonthlySpending(matching.filter { it.type == "income" }.sumOf { it.amountCents },
            expenses.sumOf { it.amountCents }, expenses.groupBy { it.categoryId.ifBlank { "Other" } }
                .map { (category, rows) -> category to rows.sumOf { it.amountCents } }.sortedByDescending { it.second })
    }
    fun budgetPercent(expense: Long, budget: Long): Int =
        if (budget <= 0) 0 else (expense.toDouble() / budget * 100).coerceIn(0.0, 100.0).toInt()
    fun changePercent(current: Long, previous: Long): Int? = if (previous <= 0) null
        else kotlin.math.round((current.toDouble() - previous) / previous * 100).toInt()
}

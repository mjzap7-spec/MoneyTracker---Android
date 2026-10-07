package com.example.moneytracker.ui.assistant

import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.ui.insights.SpendingInsights
import com.example.moneytracker.ui.transaction.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object SpendingContext {
    fun build(transactions: List<Transaction>, month: Calendar, currency: String, budget: Long): String {
        val current = SpendingInsights.calculate(transactions, month, currency)
        val previous = SpendingInsights.calculate(transactions, (month.clone() as Calendar).apply { add(Calendar.MONTH, -1) }, currency)
        return buildString {
            appendLine("Period: ${SimpleDateFormat("MMMM yyyy", Locale.US).format(month.time)}. Currency: $currency.")
            appendLine("Selected month may be incomplete. Amounts are actual recorded transactions, not predictions.")
            appendLine("Income: ${Money.format(current.income, currency)}; expenses: ${Money.format(current.expense, currency)}.")
            appendLine("Net: ${Money.format(current.income - current.expense, currency)}.")
            appendLine("Full previous month expenses: ${Money.format(previous.expense, currency)}.")
            appendLine(if (budget > 0) "Monthly budget: ${Money.format(budget, currency)}; remaining: ${Money.format(budget - current.expense, currency)}."
                else "No monthly budget set.")
            appendLine("Expense categories:")
            current.categories.forEach { (category, amount) -> appendLine("$category: ${Money.format(amount, currency)}") }
        }
    }
}

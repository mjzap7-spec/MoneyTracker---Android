package com.example.moneytracker.ui.assistant

import com.example.moneytracker.ui.transaction.Money
import java.time.LocalDate
import java.time.ZoneId

data class TransactionDraft(
    val amountCents: Long,
    val currency: String,
    val type: String,
    val category: String,
    val note: String,
    val date: LocalDate,
    val assumptions: String
) {
    val dateMillis: Long get() = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    companion object {
        val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Salary",
            "Freelance", "Gifts", "Investments", "Other")
        fun validated(amount: String, currency: String, type: String, category: String,
                      note: String, date: String, assumptions: String): TransactionDraft {
            require(currency in Money.currencies) { "Choose a supported currency." }
            require(type in listOf("income", "expense")) { "Specify income or expense." }
            require(category in categories) { "Choose a supported category." }
            val cents = requireNotNull(Money.parseCents(amount, currency)) { "Include one positive amount valid for the currency." }
            val parsedDate = try { LocalDate.parse(date) } catch (_: Exception) {
                throw IllegalArgumentException("Specify a valid transaction date.")
            }
            require(note.length <= 500 && assumptions.length <= 1000) { "Keep the transaction description short." }
            return TransactionDraft(cents, currency, type, category, note, parsedDate, assumptions)
        }
    }
}

package com.example.moneytracker.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.example.moneytracker.ui.transaction.Money

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private fun currencyKey() = "currency_${FirebaseAuth.getInstance().currentUser?.uid ?: "guest"}"
    fun currency(): String = preferences.getString(currencyKey(), "USD")
        ?.takeIf { it in Money.currencies } ?: "USD"
    fun setCurrency(code: String) {
        if (code in Money.currencies) preferences.edit().putString(currencyKey(), code).apply()
    }
    private fun budgetKey(code: String) = "budget_${FirebaseAuth.getInstance().currentUser?.uid ?: "guest"}_$code"
    fun budgetCents(code: String): Long = preferences.getLong(budgetKey(code), 0L)
    fun setBudget(code: String, cents: Long) {
        if (code in Money.currencies && cents >= 0) preferences.edit().putLong(budgetKey(code), cents).apply()
    }
    fun theme(): String = preferences.getString("theme", "dark") ?: "dark"
    fun setTheme(theme: String) { preferences.edit().putString("theme", theme).apply() }
    private fun typeKey() = "default_type_${FirebaseAuth.getInstance().currentUser?.uid ?: "guest"}"
    fun transactionType(): String = preferences.getString(typeKey(), "expense")
        ?.takeIf { it == "income" || it == "expense" } ?: "expense"
    fun setTransactionType(type: String) {
        if (type == "income" || type == "expense") preferences.edit().putString(typeKey(), type).apply()
    }
}

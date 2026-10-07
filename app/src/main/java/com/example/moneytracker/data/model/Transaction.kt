package com.example.moneytracker.data.model

data class Transaction(
    val id: String = "",
    val type: String = "expense",
    val amountCents: Long = 0L,
    val categoryId: String = "",
    val description: String = "",
    val transactionDate: Long = 0L,
    val createdAt: Long = 0L,
    val currencyCode: String = "USD"
)

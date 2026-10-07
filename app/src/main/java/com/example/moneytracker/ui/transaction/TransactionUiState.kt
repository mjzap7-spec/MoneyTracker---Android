package com.example.moneytracker.ui.transaction

import com.example.moneytracker.data.model.Transaction

data class TransactionUiState(

    val isLoading: Boolean = false,

    val isSuccess: Boolean = false,

    val transactions: List<Transaction> =
        emptyList(),

    val totalIncomeCents: Long = 0L,

    val totalExpenseCents: Long = 0L,

    val searchQuery: String = "",
    val summaryCurrencyCode: String = "USD",
    val allDates: Boolean = true,
    val listCurrencyCode: String? = null,
    val sortOrder: String = "newest",
    val categoryFilter: String? = null,
    val monthFilterMillis: Long? = null,

    val errorMessage: String? = null,

    val transactionDates: List<Long> =
        emptyList()
)

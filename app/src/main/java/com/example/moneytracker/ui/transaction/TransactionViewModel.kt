package com.example.moneytracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionViewModel : ViewModel() {

    private val repository =
        TransactionRepository()

    private var allTransactions:
            List<Transaction> =
        emptyList()

    private var selectedType =
        "all"

    private var selectedDate:
            Calendar =
        Calendar.getInstance().apply {
            set(
                Calendar.HOUR_OF_DAY,
                0
            )
            set(
                Calendar.MINUTE,
                0
            )
            set(
                Calendar.SECOND,
                0
            )
            set(
                Calendar.MILLISECOND,
                0
            )
        }

    private var summaryMode =
        "monthly"

    private val _uiState =
        MutableStateFlow(
            TransactionUiState()
        )

    val uiState:
            StateFlow<TransactionUiState> =
        _uiState.asStateFlow()

    fun addTransaction(
        transaction: Transaction
    ) {

        viewModelScope.launch {

            _uiState.value =
                TransactionUiState(
                    isLoading = true
                )

            try {

                repository.addTransaction(
                    transaction
                )

                _uiState.value =
                    TransactionUiState(
                        isSuccess = true
                    )

            } catch (e: Exception) {

                _uiState.value =
                    TransactionUiState(
                        errorMessage =
                            e.message
                                ?: "Unable to add transaction"
                    )
            }
        }
    }

    fun loadTransactions() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

            try {

                allTransactions =
                    repository
                        .getTransactions()
                        .sortedByDescending {
                            it.transactionDate
                        }

                updateTransactionDates()

                applyFilters()

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            e.message
                                ?: "Unable to load transactions"
                    )
            }
        }
    }

    fun loadTransaction(
        transactionId: String
    ) {

        viewModelScope.launch {

            try {

                val transaction =
                    repository.getTransaction(
                        transactionId
                    )

                _uiState.value =
                    TransactionUiState(
                        transactions =
                            listOf(transaction)
                    )

            } catch (e: Exception) {

                _uiState.value =
                    TransactionUiState(
                        errorMessage =
                            e.message
                                ?: "Transaction not found"
                    )
            }
        }
    }

    fun updateTransaction(
        transaction: Transaction
    ) {

        viewModelScope.launch {

            try {

                repository.updateTransaction(
                    transaction
                )

                _uiState.value =
                    TransactionUiState(
                        isSuccess = true
                    )

            } catch (e: Exception) {

                _uiState.value =
                    TransactionUiState(
                        errorMessage =
                            e.message
                                ?: "Unable to update transaction"
                    )
            }
        }
    }

    fun deleteTransaction(
        transactionId: String
    ) {

        viewModelScope.launch {

            try {

                repository.deleteTransaction(
                    transactionId
                )

                loadTransactions()

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        errorMessage =
                            e.message
                                ?: "Unable to delete transaction"
                    )
            }
        }
    }

    fun searchTransactions(
        query: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                searchQuery = query
            )

        applyFilters()
    }

    fun filterByType(
        type: String
    ) {

        selectedType = type

        applyFilters()
    }

    fun filterByDate(
        year: Int,
        month: Int,
        day: Int
    ) {

        selectedDate =
            Calendar.getInstance().apply {

                set(
                    Calendar.YEAR,
                    year
                )

                set(
                    Calendar.MONTH,
                    month
                )

                set(
                    Calendar.DAY_OF_MONTH,
                    day
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        applyFilters()
    }

    fun showMonthlySummary() {

        summaryMode =
            "monthly"

        applyFilters()
    }

    fun showYearlySummary() {

        summaryMode =
            "yearly"

        applyFilters()
    }

    fun showAllTimeSummary() {

        summaryMode =
            "all"

        applyFilters()
    }

    fun clearFilters() {

        selectedType =
            "all"

        summaryMode =
            "monthly"

        selectedDate =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        _uiState.value =
            _uiState.value.copy(
                searchQuery = ""
            )

        applyFilters()
    }

    private fun updateTransactionDates() {

        val uniqueDates =
            allTransactions
                .map { transaction ->

                    Calendar
                        .getInstance()
                        .apply {

                            timeInMillis =
                                transaction.transactionDate

                            set(
                                Calendar.HOUR_OF_DAY,
                                0
                            )

                            set(
                                Calendar.MINUTE,
                                0
                            )

                            set(
                                Calendar.SECOND,
                                0
                            )

                            set(
                                Calendar.MILLISECOND,
                                0
                            )
                        }
                        .timeInMillis
                }
                .distinct()
                .sorted()

        _uiState.value =
            _uiState.value.copy(
                transactionDates =
                    uniqueDates
            )
    }

    private fun applyFilters() {

        var filteredTransactions =
            allTransactions.filter {

                isSameDay(
                    it.transactionDate,
                    selectedDate
                )
            }

        if (selectedType != "all") {

            filteredTransactions =
                filteredTransactions.filter {
                    it.type == selectedType
                }
        }

        val query =
            _uiState.value
                .searchQuery
                .trim()

        if (query.isNotBlank()) {

            filteredTransactions =
                filteredTransactions.filter {

                    it.categoryId.contains(
                        query,
                        ignoreCase = true
                    ) ||
                            it.description.contains(
                                query,
                                ignoreCase = true
                            )
                }
        }

        val summaryTransactions =
            when (summaryMode) {

                "monthly" ->
                    allTransactions.filter {
                        isSameMonth(
                            it.transactionDate,
                            selectedDate
                        )
                    }

                "yearly" ->
                    allTransactions.filter {
                        isSameYear(
                            it.transactionDate,
                            selectedDate
                        )
                    }

                "all" ->
                    allTransactions

                else ->
                    allTransactions.filter {
                        isSameMonth(
                            it.transactionDate,
                            selectedDate
                        )
                    }
            }

        val totalIncomeCents =
            summaryTransactions
                .filter {
                    it.type == "income"
                }
                .sumOf {
                    it.amountCents
                }

        val totalExpenseCents =
            summaryTransactions
                .filter {
                    it.type == "expense"
                }
                .sumOf {
                    it.amountCents
                }

        _uiState.value =
            _uiState.value.copy(

                isLoading = false,

                transactions =
                    filteredTransactions,

                totalIncomeCents =
                    totalIncomeCents,

                totalExpenseCents =
                    totalExpenseCents
            )
    }

    private fun isSameDay(
        transactionTime: Long,
        selectedCalendar: Calendar
    ): Boolean {

        val transactionCalendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    transactionTime
            }

        return transactionCalendar.get(
            Calendar.YEAR
        ) ==
                selectedCalendar.get(
                    Calendar.YEAR
                ) &&
                transactionCalendar.get(
                    Calendar.DAY_OF_YEAR
                ) ==
                selectedCalendar.get(
                    Calendar.DAY_OF_YEAR
                )
    }

    private fun isSameMonth(
        transactionTime: Long,
        selectedCalendar: Calendar
    ): Boolean {

        val transactionCalendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    transactionTime
            }

        return transactionCalendar.get(
            Calendar.YEAR
        ) ==
                selectedCalendar.get(
                    Calendar.YEAR
                ) &&
                transactionCalendar.get(
                    Calendar.MONTH
                ) ==
                selectedCalendar.get(
                    Calendar.MONTH
                )
    }

    private fun isSameYear(
        transactionTime: Long,
        selectedCalendar: Calendar
    ): Boolean {

        val transactionCalendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    transactionTime
            }

        return transactionCalendar.get(
            Calendar.YEAR
        ) ==
                selectedCalendar.get(
                    Calendar.YEAR
                )
    }

    fun resetState() {

        _uiState.value =
            TransactionUiState()
    }
}
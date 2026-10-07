package com.example.moneytracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.util.Calendar

class TransactionViewModel : ViewModel() {

    private val repository =
        TransactionRepository()

    private var allTransactions:
            List<Transaction> =
        emptyList()

    private val deletingIds = mutableSetOf<String>()

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
        "all"

    fun selectSummaryCurrency(code: String) {
        if (code !in Money.currencies) return
        _uiState.value = _uiState.value.copy(summaryCurrencyCode = code)
        applyFilters()
    }

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
            if (_uiState.value.isLoading) return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)


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
                if (e is CancellationException) throw e

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
            if (_uiState.value.isLoading) return@launch

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

                _uiState.value = _uiState.value.copy(isLoading = false)
                applyFilters()

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "We couldn't load your transactions. Please try again."
                    )
            }
        }
    }

    fun loadTransaction(
        transactionId: String
    ) {

        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)


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
                if (e is CancellationException) throw e

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
            if (_uiState.value.isLoading) return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)


            try {

                repository.updateTransaction(
                    transaction
                )

                _uiState.value =
                    TransactionUiState(
                        isSuccess = true
                    )

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                _uiState.value =
                    _uiState.value.copy(isLoading = false,
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
            if (!deletingIds.add(transactionId)) return@launch

            try {

                repository.deleteTransaction(
                    transactionId
                )

                allTransactions = allTransactions.filterNot { it.id == transactionId }
                updateTransactionDates()
                applyFilters()

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                _uiState.value =
                    _uiState.value.copy(
                        errorMessage =
                            "We couldn't delete this transaction. Please try again."
                    )
            } finally {
                deletingIds.remove(transactionId)
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
        _uiState.value = _uiState.value.copy(allDates = false, monthFilterMillis = null)

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
        _uiState.value = _uiState.value.copy(allDates = true, listCurrencyCode = null, categoryFilter = null, monthFilterMillis = null)

        selectedType =
            "all"

        summaryMode =
            "all"

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

    fun selectedDateMillis(): Long = selectedDate.timeInMillis
    fun showAllDates() {
        _uiState.value = _uiState.value.copy(allDates = true, monthFilterMillis = null)
        applyFilters()
    }

    fun filterByMonth(monthMillis: Long?) {
        _uiState.value = _uiState.value.copy(allDates = true, monthFilterMillis = monthMillis)
        applyFilters()
    }

    fun filterByCategory(category: String?) {
        _uiState.value = _uiState.value.copy(categoryFilter = category)
        applyFilters()
    }

    fun filterByCurrency(code: String?) {
        _uiState.value = _uiState.value.copy(listCurrencyCode = code)
        applyFilters()
    }

    fun sortBy(order: String) {
        if (order !in TransactionFilters.sortOrders) return
        _uiState.value = _uiState.value.copy(sortOrder = order)
        applyFilters()
    }
    fun selectedType(): String = selectedType
    fun summaryMode(): String = summaryMode

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

        val filteredTransactions = TransactionFilters.apply(
            allTransactions,
            selectedDateMillis = if (_uiState.value.allDates) null else selectedDate.timeInMillis,
            type = selectedType,
            currencyCode = _uiState.value.listCurrencyCode,
            query = _uiState.value.searchQuery,
            category = _uiState.value.categoryFilter,
            monthMillis = _uiState.value.monthFilterMillis
        )
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
                    it.type == "income" && it.currencyCode == _uiState.value.summaryCurrencyCode
                }
                .sumOf {
                    it.amountCents
                }

        val totalExpenseCents =
            summaryTransactions
                .filter {
                    it.type == "expense" && it.currencyCode == _uiState.value.summaryCurrencyCode
                }
                .sumOf {
                    it.amountCents
                }

        _uiState.value =
            _uiState.value.copy(

                transactions =
                    TransactionFilters.sort(filteredTransactions, _uiState.value.sortOrder),

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

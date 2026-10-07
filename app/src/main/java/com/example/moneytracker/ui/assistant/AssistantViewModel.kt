package com.example.moneytracker.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneytracker.data.repository.AiAssistantRepository
import com.example.moneytracker.data.repository.TransactionRepository
import com.example.moneytracker.ui.transaction.TransactionFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.util.Calendar

data class AssistantState(val query: String = "", val draft: TransactionDraft? = null,
    val explanation: String? = null, val busy: String? = null, val error: String? = null)

class AssistantViewModel : ViewModel() {
    private val repository = AiAssistantRepository()
    private val transactions = TransactionRepository()
    private val state = MutableStateFlow(AssistantState())
    val uiState = state.asStateFlow()
    private var request: Job? = null

    fun setQuery(text: String) {
        if (text == state.value.query) return
        if (state.value.busy == "draft") { request?.cancel() }
        state.value = state.value.copy(query = text, draft = null, error = null,
            busy = if (state.value.busy == "draft") null else state.value.busy)
    }
    fun currencyChanged() {
        request?.cancel()
        state.value = state.value.copy(draft = null, explanation = null, busy = null, error = null)
    }
    fun transactionSaved() {
        request?.cancel()
        state.value = state.value.copy(draft = null, explanation = null, busy = null, error = null)
    }
    fun monthChanged() {
        if (state.value.busy == "explain") request?.cancel()
        state.value = state.value.copy(explanation = null, error = null,
            busy = if (state.value.busy == "explain") null else state.value.busy)
    }
    fun makeDraft(currency: String) {
        if (state.value.busy != null) return
        val query = state.value.query.trim()
        if (query.isEmpty()) { state.value = state.value.copy(error = "Describe one income or expense first."); return }
        state.value = state.value.copy(busy = "draft", error = null, draft = null)
        request = viewModelScope.launch {
            try {
                val result = repository.draft(query, currency, LocalDate.now())
                state.value = state.value.copy(draft = result, busy = null)
            } catch (e: Exception) { handleError(e) }
        }
    }
    fun explain(month: Calendar, currency: String, budget: Long) {
        if (state.value.busy != null) return
        val selectedMonth = month.clone() as Calendar
        state.value = state.value.copy(busy = "explain", error = null, explanation = null)
        request = viewModelScope.launch {
            try {
                withTimeout(45_000) {
                    val records = transactions.getTransactions()
                    if (TransactionFilters.apply(records, currencyCode = currency, monthMillis = selectedMonth.timeInMillis).isEmpty()) {
                        state.value = state.value.copy(busy = null, error = "No transactions in this month and currency. Choose another month or add a transaction.")
                        return@withTimeout
                    }
                    val explanation = repository.explain(SpendingContext.build(records, selectedMonth, currency, budget))
                    state.value = state.value.copy(explanation = explanation, busy = null)
                }
            } catch (e: Exception) { handleError(e) }
        }
    }
    private fun handleError(error: Exception) {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
        val message = when (error) {
            is TimeoutCancellationException -> "AI took too long to respond. Please try again."
            is IllegalArgumentException -> error.message ?: "Please describe one transaction more clearly."
            else -> "AI is unavailable. Check your connection and that Firebase AI Logic is enabled for this project, then try again."
        }
        state.value = state.value.copy(busy = null, error = message)
    }
}

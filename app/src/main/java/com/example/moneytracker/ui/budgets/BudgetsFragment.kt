package com.example.moneytracker.ui.budgets

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.R
import com.example.moneytracker.data.repository.AppPreferences
import com.example.moneytracker.databinding.FragmentBudgetsBinding
import com.example.moneytracker.ui.insights.SpendingInsights
import com.example.moneytracker.ui.transaction.CurrencyPicker
import com.example.moneytracker.ui.transaction.Money
import com.example.moneytracker.ui.transaction.TransactionViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetsFragment : Fragment(R.layout.fragment_budgets) {
    private var viewBinding: FragmentBudgetsBinding? = null
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var preferences: AppPreferences
    private val month = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var currency = "USD"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentBudgetsBinding.bind(view)
        viewBinding = binding
        preferences = AppPreferences(requireContext())
        binding.root.requestFocus()
        month.timeInMillis = savedInstanceState?.getLong("month") ?: arguments?.getLong("month") ?: month.timeInMillis
        currency = savedInstanceState?.getString("currency") ?: arguments?.getString("currency") ?: preferences.currency()
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.previousButton.setOnClickListener { month.add(Calendar.MONTH, -1); render() }
        binding.nextButton.setOnClickListener { month.add(Calendar.MONTH, 1); render() }
        showEditor()
        CurrencyPicker.setup(binding.currencySpinner, currency) {
            if (currency != it) { currency = it; showEditor() }
        }
        binding.budgetEdit.doAfterTextChanged { binding.budgetInput.error = null }
        binding.saveButton.setOnClickListener {
            val amount = Money.parseCents(binding.budgetEdit.text.toString(), currency)
            if (amount == null) {
                binding.budgetInput.error = getString(R.string.invalid_budget)
                binding.budgetEdit.requestFocus()
            } else {
                preferences.setBudget(currency, amount)
                binding.budgetEdit.clearFocus()
                androidx.core.view.WindowCompat.getInsetsController(requireActivity().window, view)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
                render(); showEditor()
                Snackbar.make(view, R.string.budget_saved, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.removeButton.setOnClickListener {
            preferences.setBudget(currency, 0); render(); showEditor()
            Snackbar.make(view, R.string.budget_removed, Snackbar.LENGTH_SHORT).show()
        }
        binding.retryButton.setOnClickListener { viewModel.loadTransactions() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect { render() } }
        }
    }

    private fun showEditor() {
        val binding = viewBinding ?: return
        binding.budgetInput.hint = "${getString(R.string.budget_amount)} · $currency"
        binding.budgetInput.helperText = getString(if (java.util.Currency.getInstance(currency).defaultFractionDigits == 0)
            R.string.amount_hint_whole else R.string.amount_hint_decimal)
        binding.budgetEdit.setText(preferences.budgetCents(currency).takeIf { it > 0 }?.let(Money::editable).orEmpty())
        binding.removeButton.isEnabled = preferences.budgetCents(currency) > 0
        binding.budgetInput.error = null
    }

    private fun render() {
        val binding = viewBinding ?: return
        val state = viewModel.uiState.value
        binding.monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
        binding.statusLabel.isVisible = state.isLoading || state.errorMessage != null
        binding.statusLabel.text = if (state.isLoading) getString(R.string.loading_transactions) else state.errorMessage
        binding.retryButton.isVisible = state.errorMessage != null && !state.isLoading
        binding.budgetContainer.removeAllViews()
        val currencies = Money.currencies.filter { preferences.budgetCents(it) > 0 }
        binding.emptyLabel.isVisible = currencies.isEmpty()
        // Avoid presenting a zero spend as a real result before transactions load.
        if (state.isLoading || state.errorMessage != null) return
        currencies.forEach { code ->
            val limit = preferences.budgetCents(code)
            val spent = SpendingInsights.calculate(state.transactions, month, code).expense
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_section_card)
                val padding = dp(16); setPadding(padding, padding, padding, padding)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            }
            card.addView(TextView(requireContext()).apply {
                text = getString(R.string.budget_card_title, code, Money.format(limit, code)); textSize = 18f
                setTextColor(context.getColor(R.color.text_primary)); setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(TextView(requireContext()).apply {
                text = getString(R.string.budget_spent, Money.format(spent, code)); textSize = 14f
                setTextColor(context.getColor(R.color.text_secondary)); setPadding(0, dp(8), 0, dp(8))
            })
            card.addView(LinearProgressIndicator(requireContext()).apply {
                progress = SpendingInsights.budgetPercent(spent, limit)
                setIndicatorColor(context.getColor(if (spent > limit) R.color.expense_red else R.color.primary_blue))
                layoutParams = LinearLayout.LayoutParams(-1, dp(8))
            })
            card.addView(TextView(requireContext()).apply {
                text = if (spent > limit) getString(R.string.budget_exceeded, Money.format(spent - limit, code), Money.format(limit, code))
                    else getString(R.string.budget_remaining, Money.format(limit - spent, code), Money.format(limit, code))
                setTextColor(context.getColor(if (spent > limit) R.color.expense_red else R.color.text_secondary))
                setPadding(0, dp(8), 0, 0)
            })
            binding.budgetContainer.addView(card)
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onResume() { super.onResume(); viewModel.loadTransactions() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); outState.putLong("month", month.timeInMillis); outState.putString("currency", currency)
    }
    override fun onDestroyView() { viewBinding = null; super.onDestroyView() }
}

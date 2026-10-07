package com.example.moneytracker.ui.insights

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentInsightsBinding
import com.example.moneytracker.data.repository.AppPreferences
import com.example.moneytracker.ui.transaction.CurrencyPicker
import com.example.moneytracker.ui.transaction.Money
import com.example.moneytracker.ui.transaction.TransactionViewModel
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InsightsFragment : Fragment(R.layout.fragment_insights) {
    private var viewBinding: FragmentInsightsBinding? = null
    private val viewModel: TransactionViewModel by viewModels()
    private val month = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var currency = "USD"
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentInsightsBinding.bind(view)
        viewBinding = binding
        binding.root.requestFocus()
        savedInstanceState?.getLong("month")?.let { month.timeInMillis = it }
        currency = savedInstanceState?.getString("currency") ?: AppPreferences(requireContext()).currency()
        CurrencyPicker.setup(binding.currencySpinner, currency, showNames = false) { currency = it; render() }
        binding.previousButton.setOnClickListener { month.add(Calendar.MONTH, -1); render() }
        binding.nextButton.setOnClickListener { month.add(Calendar.MONTH, 1); render() }
        binding.budgetButton.setOnClickListener {
            findNavController().navigate(R.id.budgetsFragment, Bundle().apply {
                putString("currency", currency); putLong("month", month.timeInMillis)
            })
        }
        binding.retryButton.setOnClickListener { viewModel.loadTransactions() }
        binding.assistantButton.setOnClickListener {
            findNavController().navigate(R.id.assistantFragment, Bundle().apply {
                putString("currency", currency); putLong("month", month.timeInMillis)
            })
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render() }
            }
        }
    }
    private fun render() {
        val binding = viewBinding ?: return
        val state = viewModel.uiState.value
        val summary = SpendingInsights.calculate(state.transactions, month, currency)
        val previousMonth = (month.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val previous = SpendingInsights.calculate(state.transactions, previousMonth, currency)
        val change = SpendingInsights.changePercent(summary.expense, previous.expense)
        binding.trendLabel.text = when {
            summary.expense == previous.expense -> getString(R.string.trend_same)
            change == null -> getString(R.string.trend_no_previous, Money.format(summary.expense, currency))
            summary.expense > previous.expense -> getString(R.string.trend_up,
                Money.format(summary.expense - previous.expense, currency), change)
            else -> getString(R.string.trend_down, Money.format(previous.expense - summary.expense, currency), -change)
        }
        binding.trendLabel.isVisible = !state.isLoading && state.errorMessage == null
        binding.monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
        binding.totalsLabel.text = getString(R.string.month_totals, Money.format(summary.income, currency),
            Money.format(summary.expense, currency), Money.format(summary.income - summary.expense, currency))
        val budget = AppPreferences(requireContext()).budgetCents(currency)
        binding.budgetLabel.text = when {
            budget == 0L -> getString(R.string.budget_unset)
            summary.expense > budget -> getString(R.string.budget_exceeded,
                Money.format(summary.expense - budget, currency), Money.format(budget, currency))
            else -> getString(R.string.budget_remaining,
                Money.format(budget - summary.expense, currency), Money.format(budget, currency))
        }
        binding.budgetProgress.isVisible = budget > 0
        binding.budgetProgress.setProgressCompat(SpendingInsights.budgetPercent(summary.expense, budget), true)
        binding.budgetProgress.setIndicatorColor(ContextCompat.getColor(requireContext(),
            if (summary.expense > budget) R.color.expense_red else R.color.primary_blue))
        binding.categoryContainer.removeAllViews()
        if (summary.categories.isEmpty() && !state.isLoading) addLabel(getString(R.string.no_month_expenses))
        summary.categories.forEach { (name, amount) ->
            val percent = SpendingInsights.budgetPercent(amount, summary.expense)
            addLabel(getString(R.string.category_amount, name, Money.format(amount, currency), percent))
            val bar = LinearProgressIndicator(requireContext()).apply {
                progress = percent
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    (8 * resources.displayMetrics.density).toInt())
            }
            binding.categoryContainer.addView(bar)
        }
        binding.statusLabel.isVisible = state.isLoading || state.errorMessage != null
        binding.statusLabel.text = if (state.isLoading) getString(R.string.loading_transactions) else state.errorMessage
        binding.retryButton.isVisible = state.errorMessage != null && !state.isLoading
    }
    private fun addLabel(value: String) {
        val label = TextView(requireContext()).apply {
            text = value; textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            val padding = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, padding, 0, padding)
        }
        viewBinding?.categoryContainer?.addView(label)
    }
    override fun onResume() { super.onResume(); viewModel.loadTransactions(); render() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("month", month.timeInMillis); outState.putString("currency", currency)
    }
    override fun onDestroyView() { viewBinding = null; super.onDestroyView() }
}

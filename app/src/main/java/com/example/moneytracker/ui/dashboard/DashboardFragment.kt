package com.example.moneytracker.ui.dashboard

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentDashboardBinding
import com.example.moneytracker.ui.transaction.CurrencyPicker
import com.example.moneytracker.ui.transaction.Money
import com.example.moneytracker.ui.transaction.TransactionAdapter
import com.example.moneytracker.ui.transaction.TransactionViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    private val viewModel: TransactionViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.helpButton.setOnClickListener { findNavController().navigate(R.id.helpFragment) }
        adapter = TransactionAdapter({}, { transaction ->
            findNavController().navigate(R.id.editTransactionFragment,
                Bundle().apply { putString("transactionId", transaction.id) })
        }, showActions = false)
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = adapter
        val preferences = com.example.moneytracker.data.repository.AppPreferences(requireContext())
        viewModel.selectSummaryCurrency(preferences.currency())
        CurrencyPicker.setup(binding.summaryCurrencySpinner,
            viewModel.uiState.value.summaryCurrencyCode, showNames = false) {
                preferences.setCurrency(it)
                viewModel.selectSummaryCurrency(it)
            }
        binding.summaryToggleGroup.check(when (viewModel.summaryMode()) {
            "monthly" -> R.id.monthlyButton
            "yearly" -> R.id.yearlyButton
            else -> R.id.allTimeButton
        })
        binding.summaryToggleGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked) when (id) {
                R.id.monthlyButton -> viewModel.showMonthlySummary()
                R.id.yearlyButton -> viewModel.showYearlySummary()
                R.id.allTimeButton -> viewModel.showAllTimeSummary()
            }
        }
        binding.addTransactionButton.setOnClickListener {
            findNavController().navigate(R.id.addTransactionFragment, Bundle().apply {
                putLong("selectedDate", System.currentTimeMillis())
                putString("currencyCode", viewModel.uiState.value.summaryCurrencyCode)
            })
        }
        binding.currencyConverterButton.setOnClickListener {
            findNavController().navigate(R.id.currencyFragment, Bundle().apply {
                putString("currencyCode", viewModel.uiState.value.summaryCurrencyCode)
            })
        }
        binding.assistantButton.setOnClickListener {
            findNavController().navigate(R.id.assistantFragment, Bundle().apply {
                putString("currency", viewModel.uiState.value.summaryCurrencyCode)
            })
        }
        binding.viewHistoryButton.setOnClickListener {
            (requireActivity() as com.example.moneytracker.MainActivity).selectMainTab(R.id.historyFragment)
        }
        binding.retryButton.setOnClickListener { viewModel.loadTransactions() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val code = state.summaryCurrencyCode
                    fun amount(cents: Long) = SpannableString(Money.format(cents, code)).apply {
                        setSpan(RelativeSizeSpan(0.5f), 0, code.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    binding.incomeTextView.text = amount(state.totalIncomeCents)
                    binding.expenseTextView.text = amount(state.totalExpenseCents)
                    binding.balanceTextView.text = amount(state.totalIncomeCents - state.totalExpenseCents)
                    val period = when (viewModel.summaryMode()) {
                        "monthly" -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                        "yearly" -> Calendar.getInstance().get(Calendar.YEAR).toString()
                        else -> null
                    }
                    binding.summaryPeriodTextView.text = if (period == null)
                        getString(R.string.summary_all_time, code) else getString(R.string.summary_period, period, code)
                    adapter.submitList(state.transactions.filter { it.currencyCode == code }.take(3))
                    binding.emptyTextView.visibility = if (adapter.itemCount == 0 && !state.isLoading && state.errorMessage == null) View.VISIBLE else View.GONE
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.errorTextView.text = state.errorMessage
                    binding.errorTextView.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
                    binding.retryButton.visibility = binding.errorTextView.visibility
                    binding.retryButton.isEnabled = !state.isLoading
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currency = com.example.moneytracker.data.repository.AppPreferences(requireContext()).currency()
        viewModel.selectSummaryCurrency(currency)
        binding.summaryCurrencySpinner.setSelection(Money.currencies.indexOf(currency).coerceAtLeast(0))
        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("successMessage")?.let {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        }
        viewModel.loadTransactions()
    }

    override fun onDestroyView() {
        binding.transactionRecyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }
}

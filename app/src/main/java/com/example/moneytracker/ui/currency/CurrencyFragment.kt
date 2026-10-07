package com.example.moneytracker.ui.currency

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentCurrencyBinding
import com.example.moneytracker.ui.transaction.CurrencyPicker
import com.example.moneytracker.ui.transaction.Money
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrencyFragment : Fragment() {
    private val viewModel: CurrencyViewModel by viewModels()
    private var _binding: FragmentCurrencyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCurrencyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initialize(arguments?.getString("currencyCode") ?: "USD")
        binding.root.requestFocus()
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.refreshButton.setOnClickListener { viewModel.refresh() }
        binding.swapButton.setOnClickListener { viewModel.swap() }
        binding.providerTextView.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.exchangerate-api.com")))
        }
        CurrencyPicker.setup(binding.fromSpinner, viewModel.uiState.value.from) { viewModel.selectFrom(it) }
        CurrencyPicker.setup(binding.toSpinner, viewModel.uiState.value.to) { viewModel.selectTo(it) }
        binding.amountEditText.setText(viewModel.uiState.value.amount)
        binding.amountEditText.doAfterTextChanged { viewModel.setAmount(it?.toString().orEmpty()) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val fromIndex = Money.currencies.indexOf(state.from)
                    val toIndex = Money.currencies.indexOf(state.to)
                    if (binding.fromSpinner.selectedItemPosition != fromIndex) binding.fromSpinner.setSelection(fromIndex)
                    if (binding.toSpinner.selectedItemPosition != toIndex) binding.toSpinner.setSelection(toIndex)
                    binding.amountInputLayout.prefixText = state.from
                    binding.amountInputLayout.helperText = getString(if (java.util.Currency.getInstance(state.from).defaultFractionDigits == 0)
                        R.string.amount_hint_whole else R.string.amount_hint_decimal)
                    val cents = Money.parseCents(state.amount, state.from)
                    binding.amountInputLayout.error = if (state.amount.isNotBlank() && cents == null)
                        getString(R.string.invalid_conversion_amount) else null
                    val rates = state.rates
                    binding.resultTextView.text = if (rates != null && cents != null)
                        Money.formatAmount(rates.convert(BigDecimal.valueOf(cents, 2), state.from, state.to), state.to)
                        else getString(R.string.conversion_unavailable)
                    val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 6 }
                    binding.rateTextView.text = if (rates != null)
                        getString(R.string.exchange_rate_pair, state.from, numberFormat.format(rates.rate(state.from, state.to)), state.to)
                        else getString(R.string.rates_waiting)
                    binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
                    binding.refreshButton.isEnabled = !state.loading
                    binding.refreshButton.setText(if (state.loading) R.string.rates_updating else R.string.refresh_rates)
                    binding.errorTextView.text = state.error
                    binding.errorTextView.visibility = if (state.error != null) View.VISIBLE else View.GONE
                    val updated = rates?.let { SimpleDateFormat("MMM d, yyyy 'at' h:mm a z", Locale.getDefault()).format(Date(it.updatedAtMillis)) }
                    binding.statusTextView.text = when {
                        rates == null && state.loading -> getString(R.string.rates_updating)
                        rates == null -> getString(R.string.rates_unavailable)
                        state.error != null || System.currentTimeMillis() >= rates.nextUpdateMillis -> getString(R.string.rates_cached, updated)
                        else -> getString(R.string.rates_updated, updated)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.moneytracker.ui.assistant

import android.os.Bundle
import android.view.View
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
import com.example.moneytracker.databinding.FragmentAssistantBinding
import com.example.moneytracker.ui.transaction.CurrencyPicker
import com.example.moneytracker.ui.transaction.Money
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class AssistantFragment : Fragment(R.layout.fragment_assistant) {
    private val viewModel: AssistantViewModel by viewModels()
    private val month = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var currency = "USD"
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAssistantBinding.bind(view)
        val preferences = AppPreferences(requireContext())
        binding.root.requestFocus()
        month.timeInMillis = savedInstanceState?.getLong("month") ?: arguments?.getLong("month") ?: month.timeInMillis
        currency = savedInstanceState?.getString("currency") ?: arguments?.getString("currency") ?: preferences.currency()
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        CurrencyPicker.setup(binding.currencySpinner, currency) {
            if (currency != it) { currency = it; viewModel.currencyChanged() }
        }
        binding.promptEdit.setText(viewModel.uiState.value.query)
        binding.promptEdit.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.exampleButton.setOnClickListener { binding.promptEdit.setText(getString(R.string.ai_example)) }
        binding.generateButton.setOnClickListener {
            binding.promptEdit.clearFocus()
            androidx.core.view.WindowCompat.getInsetsController(requireActivity().window, view)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            viewModel.makeDraft(currency)
        }
        binding.reviewButton.setOnClickListener {
            viewModel.uiState.value.draft?.let { draft ->
                findNavController().navigate(R.id.addTransactionFragment, Bundle().apply {
                    putLong("draftAmount", draft.amountCents); putString("draftType", draft.type)
                    putString("draftCategory", draft.category); putString("draftNote", draft.note)
                    putString("currencyCode", draft.currency); putLong("selectedDate", draft.dateMillis)
                })
            }
        }
        fun showMonth() { binding.monthLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time) }
        showMonth()
        binding.previousButton.setOnClickListener { month.add(Calendar.MONTH, -1); showMonth(); viewModel.monthChanged() }
        binding.nextButton.setOnClickListener { month.add(Calendar.MONTH, 1); showMonth(); viewModel.monthChanged() }
        binding.explainButton.setOnClickListener { viewModel.explain(month, currency, preferences.budgetCents(currency)) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var shownError: String? = null
                viewModel.uiState.collect { state ->
                    if (state.error != null && state.error != shownError) {
                        Snackbar.make(view, state.error, Snackbar.LENGTH_LONG).show()
                    }
                    shownError = state.error
                    binding.generateButton.isEnabled = state.busy == null
                    binding.explainButton.isEnabled = state.busy == null
                    binding.progressBar.isVisible = state.busy != null
                    binding.errorLabel.isVisible = state.error != null
                    binding.errorLabel.text = state.error
                    binding.draftCard.isVisible = state.draft != null
                    state.draft?.let { draft ->
                        binding.draftLabel.text = getString(R.string.ai_draft_summary,
                            getString(if (draft.type == "income") R.string.income else R.string.expense),
                            Money.format(draft.amountCents, draft.currency), draft.category,
                            draft.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())), draft.note)
                        binding.assumptionsLabel.text = draft.assumptions
                        binding.assumptionsLabel.isVisible = draft.assumptions.isNotBlank()
                    }
                    binding.explanationLabel.isVisible = state.explanation != null
                    binding.explanationLabel.text = state.explanation
                    binding.generateButton.setText(if (state.busy == "draft") R.string.ai_working else R.string.ai_generate)
                    binding.explainButton.setText(if (state.busy == "explain") R.string.ai_working else R.string.ai_explain)
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("successMessage")?.let {
            viewModel.transactionSaved()
            Snackbar.make(requireView(), it, Snackbar.LENGTH_SHORT).show()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); outState.putLong("month", month.timeInMillis); outState.putString("currency", currency)
    }
}

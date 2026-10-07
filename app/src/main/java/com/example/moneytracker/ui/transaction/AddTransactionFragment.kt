package com.example.moneytracker.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.R
import com.example.moneytracker.data.model.Transaction
import com.example.moneytracker.databinding.FragmentAddTransactionBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class AddTransactionFragment :
    Fragment() {

    private val viewModel:
            TransactionViewModel by viewModels()

    private var _binding:
            FragmentAddTransactionBinding? =
        null

    private val binding
        get() = _binding!!

    private var selectedDate =
        System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAddTransactionBinding
                .inflate(
                    inflater,
                    container,
                    false
                )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        selectedDate = savedInstanceState?.getLong("selectedDate") ?: arguments?.getLong("selectedDate") ?: selectedDate
        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
        if (savedInstanceState == null) {
            binding.typeRadioGroup.check(if ((arguments?.getString("draftType") ?: com.example.moneytracker.data.repository.AppPreferences(requireContext()).transactionType()) == "income")
                R.id.incomeRadioButton else R.id.expenseRadioButton)
        }
        binding.amountEditText.doAfterTextChanged { binding.amountInputLayout.error = null }
        CurrencyPicker.setup(binding.currencySpinner, savedInstanceState?.getString("currencyCode") ?: arguments?.getString("currencyCode") ?: com.example.moneytracker.data.repository.AppPreferences(requireContext()).currency()) { code ->
            binding.amountInputLayout.prefixText = code
            binding.amountInputLayout.error = null
            binding.amountInputLayout.helperText = getString(if (java.util.Currency.getInstance(code).defaultFractionDigits == 0) R.string.amount_hint_whole else R.string.amount_hint_decimal)
        }
        setupCategorySpinner()
        if (savedInstanceState == null && arguments?.containsKey("draftAmount") == true) {
            binding.amountEditText.setText(Money.editable(requireArguments().getLong("draftAmount")))
            binding.descriptionEditText.setText(arguments?.getString("draftNote").orEmpty())
            val categories = resources.getStringArray(R.array.transaction_categories)
            binding.categorySpinner.setSelection(categories.indexOf(arguments?.getString("draftCategory")).coerceAtLeast(0))
        }
        setupDate()
        setupAddButton()
        observeUiState()
    }

    private fun setupCategorySpinner() {

        val categories =
            resources.getStringArray(
                R.array.transaction_categories
            )

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_spinner_item,
                categories
            )

        adapter.setDropDownViewResource(
            android.R.layout
                .simple_spinner_dropdown_item
        )

        binding.categorySpinner.adapter =
            adapter
    }

    private fun setupDate() {

        updateDateText()

        binding.selectDateButton
            .setOnClickListener {

                val calendar =
                    Calendar.getInstance()

                calendar.timeInMillis =
                    selectedDate

                DatePickerDialog(
                    requireContext(),

                    { _, year,
                      month,
                      dayOfMonth ->

                        calendar.set(
                            year,
                            month,
                            dayOfMonth
                        )

                        selectedDate =
                            calendar.timeInMillis

                        updateDateText()
                    },

                    calendar.get(
                        Calendar.YEAR
                    ),

                    calendar.get(
                        Calendar.MONTH
                    ),

                    calendar.get(
                        Calendar.DAY_OF_MONTH
                    )
                ).show()
            }
    }

    private fun updateDateText() {
        binding.dateTextView.text = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDate))
    }

    private fun setupAddButton() {

        binding.addTransactionButton
            .setOnClickListener {

                binding.errorTextView
                    .visibility =
                    View.GONE

                val amountCents = Money.parseCents(binding.amountEditText.text.toString(), CurrencyPicker.selected(binding.currencySpinner))

                if (
                    amountCents == null
                ) {

                    binding.amountInputLayout.error = "Enter a positive amount valid for this currency"
                    binding.amountEditText.requestFocus()

                    return@setOnClickListener
                }

                val category =
                    binding.categorySpinner
                        .selectedItem
                        .toString()

                val description =
                    binding
                        .descriptionEditText
                        .text
                        .toString()
                        .trim()

                val type =
                    if (
                        binding
                            .incomeRadioButton
                            .isChecked
                    ) {
                        "income"
                    } else {
                        "expense"
                    }

                val transaction =
                    Transaction(

                        type = type,

                        amountCents = amountCents,
                        currencyCode = CurrencyPicker.selected(binding.currencySpinner),

                        categoryId =
                            category,

                        description =
                            description,

                        transactionDate =
                            selectedDate,

                        createdAt =
                            System.currentTimeMillis()
                    )

                viewModel.addTransaction(
                    transaction
                )
            }
    }

    private fun observeUiState() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        viewModel.uiState
                            .collect { state ->

                                binding.addTransactionButton.setText(if (state.isLoading) R.string.saving else R.string.add_transaction)
                                binding.addTransactionButton.isEnabled = !state.isLoading
                                binding.progressBar
                                    .visibility =
                                    if (
                                        state.isLoading
                                    ) {
                                        View.VISIBLE
                                    } else {
                                        View.GONE
                                    }

                                if (
                                    state.errorMessage !=
                                    null
                                ) {

                                    binding.errorTextView
                                        .text =
                                        state.errorMessage

                                    binding.errorTextView
                                        .visibility =
                                        View.VISIBLE

                                } else {

                                    binding.errorTextView
                                        .visibility =
                                        View.GONE
                                }

                                if (
                                    state.isSuccess
                                ) {

                                    findNavController().previousBackStackEntry?.savedStateHandle?.set("successMessage", getString(R.string.transaction_added))
                                    viewModel.resetState()

                                    findNavController()
                                        .popBackStack()
                                }
                            }
                    }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("selectedDate", selectedDate)
        _binding?.let { outState.putString("currencyCode", CurrencyPicker.selected(it.currencySpinner)) }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}

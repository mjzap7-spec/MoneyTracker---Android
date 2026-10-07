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
import com.example.moneytracker.databinding.FragmentEditTransactionBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class EditTransactionFragment :
    Fragment() {

    private val viewModel:
            TransactionViewModel by viewModels()

    private var _binding:
            FragmentEditTransactionBinding? =
        null

    private val binding
        get() = _binding!!

    private lateinit var categoryAdapter:
            ArrayAdapter<CharSequence>

    private var selectedDate =
        System.currentTimeMillis()

    private var populatedTransactionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentEditTransactionBinding
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

        val transactionId =
            arguments?.getString(
                "transactionId"
            )

        if (
            transactionId == null
        ) {

            findNavController()
                .popBackStack()

            return
        }

        populatedTransactionId = savedInstanceState?.getString("populatedTransactionId")
        selectedDate = savedInstanceState?.getLong("selectedDate") ?: selectedDate
        if (populatedTransactionId != null) updateDateText()
        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
        binding.amountEditText.doAfterTextChanged { binding.amountInputLayout.error = null }
        CurrencyPicker.setup(binding.currencySpinner, savedInstanceState?.getString("currencyCode") ?: "USD") { code ->
            binding.amountInputLayout.prefixText = code
            binding.amountInputLayout.error = null
            binding.amountInputLayout.helperText = getString(if (java.util.Currency.getInstance(code).defaultFractionDigits == 0) R.string.amount_hint_whole else R.string.amount_hint_decimal)
        }
        setupCategorySpinner()
        setupDatePicker()
        setupSaveButton()
        observeUiState()

        viewModel.loadTransaction(
            transactionId
        )
    }

    private fun setupCategorySpinner() {

        categoryAdapter =
            ArrayAdapter.createFromResource(

                requireContext(),

                R.array
                    .transaction_categories,

                android.R.layout
                    .simple_spinner_item
            )

        categoryAdapter
            .setDropDownViewResource(
                android.R.layout
                    .simple_spinner_dropdown_item
            )

        binding.categorySpinner.adapter =
            categoryAdapter
    }

    private fun setupDatePicker() {

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

    private fun setupSaveButton() {

        binding.saveButton
            .setOnClickListener {

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
                    binding.descriptionEditText
                        .text
                        .toString()
                        .trim()

                val transaction =
                    viewModel.uiState.value
                        .transactions
                        .firstOrNull()

                if (
                    transaction == null
                ) {
                    return@setOnClickListener
                }

                val updatedTransaction =
                    transaction.copy(type = if (binding.incomeRadioButton.isChecked) "income" else "expense",

                        amountCents = amountCents,
                        currencyCode = CurrencyPicker.selected(binding.currencySpinner),

                        categoryId =
                            category,

                        description =
                            description,

                        transactionDate =
                            selectedDate
                    )

                viewModel.updateTransaction(
                    updatedTransaction
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

                                binding.saveButton.setText(if (state.isLoading) R.string.saving else R.string.save_changes)
                                binding.saveButton.isEnabled = !state.isLoading && state.transactions.isNotEmpty()
                                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                                val transaction =
                                    state.transactions
                                        .firstOrNull()

                                if (
                                    state.isSuccess
                                ) {

                                    findNavController().previousBackStackEntry?.savedStateHandle?.set("successMessage", getString(R.string.transaction_updated))
                                    viewModel.resetState()

                                    findNavController()
                                        .popBackStack()

                                    return@collect
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
                                    binding.errorTextView.visibility = View.GONE
                                }

                                if (
                                    transaction != null && populatedTransactionId != transaction.id
                                ) {

                                    populatedTransactionId = transaction.id
                                    binding.currencySpinner.setSelection(Money.currencies.indexOf(transaction.currencyCode).coerceAtLeast(0))
                                    binding.amountInputLayout.prefixText = transaction.currencyCode

                                    selectedDate =
                                        transaction
                                            .transactionDate

                                    updateDateText()

                                    binding
                                        .amountEditText
                                        .setText(
                                            Money.editable(transaction.amountCents)
                                        )

                                    val position =
                                        categoryAdapter
                                            .getPosition(
                                                transaction
                                                    .categoryId
                                            )

                                    if (
                                        position >= 0
                                    ) {

                                        binding
                                            .categorySpinner
                                            .setSelection(
                                                position
                                            )
                                    }

                                    binding
                                        .descriptionEditText
                                        .setText(
                                            transaction
                                                .description
                                        )

                                    if (
                                        transaction.type ==
                                        "income"
                                    ) {

                                        binding
                                            .incomeRadioButton
                                            .isChecked =
                                            true

                                    } else {

                                        binding
                                            .expenseRadioButton
                                            .isChecked =
                                            true
                                    }
                                }
                            }
                    }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("populatedTransactionId", populatedTransactionId)
        outState.putLong("selectedDate", selectedDate)
        _binding?.let { outState.putString("currencyCode", CurrencyPicker.selected(it.currencySpinner)) }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}

package com.example.moneytracker.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            selectedDate

        binding.dateTextView.text =
            "Date: %02d/%02d/%04d".format(

                calendar.get(
                    Calendar.MONTH
                ) + 1,

                calendar.get(
                    Calendar.DAY_OF_MONTH
                ),

                calendar.get(
                    Calendar.YEAR
                )
            )
    }

    private fun setupSaveButton() {

        binding.saveButton
            .setOnClickListener {

                val amount =
                    binding.amountEditText
                        .text
                        .toString()
                        .trim()
                        .toDoubleOrNull()

                if (
                    amount == null ||
                    amount <= 0
                ) {

                    binding.errorTextView.text =
                        "Enter a valid amount"

                    binding.errorTextView
                        .visibility =
                        View.VISIBLE

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
                    transaction.copy(

                        amountCents =
                            (amount * 100)
                                .toLong(),

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

                                val transaction =
                                    state.transactions
                                        .firstOrNull()

                                if (
                                    state.isSuccess
                                ) {

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
                                }

                                if (
                                    transaction != null
                                ) {

                                    selectedDate =
                                        transaction
                                            .transactionDate

                                    updateDateText()

                                    binding
                                        .amountEditText
                                        .setText(
                                            (
                                                    transaction
                                                        .amountCents /
                                                            100.0
                                                    ).toString()
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

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
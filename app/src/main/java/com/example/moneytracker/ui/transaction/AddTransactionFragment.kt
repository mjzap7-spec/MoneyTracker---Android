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

        setupCategorySpinner()
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

        val calendar =
            Calendar.getInstance()

        calendar.timeInMillis =
            selectedDate

        binding.dateTextView.text =
            "%02d/%02d/%04d".format(

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

    private fun setupAddButton() {

        binding.addTransactionButton
            .setOnClickListener {

                binding.errorTextView
                    .visibility =
                    View.GONE

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

                        amountCents =
                            (amount * 100)
                                .toLong(),

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

                                    viewModel.resetState()

                                    findNavController()
                                        .popBackStack()
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
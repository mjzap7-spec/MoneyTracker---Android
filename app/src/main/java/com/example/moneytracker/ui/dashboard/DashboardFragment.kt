package com.example.moneytracker.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentDashboardBinding
import com.example.moneytracker.ui.auth.AuthViewModel
import com.example.moneytracker.ui.transaction.TransactionAdapter
import com.example.moneytracker.ui.transaction.TransactionViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private val viewModel:
            TransactionViewModel by viewModels()

    private val authViewModel:
            AuthViewModel by viewModels()

    private var _binding:
            FragmentDashboardBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var transactionAdapter:
            TransactionAdapter

    /**
     * The date currently selected by the user.
     *
     * We keep this date separately from the DatePickerDialog
     * because the selected date is also used by the ViewModel
     * to filter transactions and calculate the summary.
     */
    private var selectedDate:
            Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDashboardBinding.inflate(
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

        /*
         * Make sure the initial selected date
         * starts at midnight.
         */
        selectedDate = Calendar.getInstance().apply {

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )
        }

        setupRecyclerView()

        setupSummaryButtons()

        setupCalendar()

        setupSearch()

        setupTypeFilter()

        setupClearFilters()

        setupAddButton()

        setupLogout()

        /*
         * Start observing the ViewModel.
         */
        observeUiState()

        /*
         * Load transactions from Firestore.
         */
        viewModel.loadTransactions()
    }

    // ================================================================
    // RecyclerView
    // ================================================================

    private fun setupRecyclerView() {

        transactionAdapter =
            TransactionAdapter(

                onDeleteClick = { transaction ->

                    showDeleteDialog(
                        transaction.id
                    )
                },

                onEditClick = { transaction ->

                    val bundle =
                        Bundle().apply {

                            putString(
                                "transactionId",
                                transaction.id
                            )
                        }

                    findNavController().navigate(
                        R.id.action_dashboardFragment_to_editTransactionFragment,
                        bundle
                    )
                }
            )

        binding.transactionRecyclerView.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext()
                )

            adapter =
                transactionAdapter

            /*
             * The RecyclerView is inside a NestedScrollView.
             *
             * Therefore, let the outer NestedScrollView
             * handle the scrolling.
             */
            isNestedScrollingEnabled = false
        }
    }

    // ================================================================
    // Summary buttons
    // ================================================================

    private fun setupSummaryButtons() {

        /*
         * Monthly is selected when Dashboard opens.
         */
        binding.summaryToggleGroup.check(
            R.id.monthlyButton
        )

        binding.summaryToggleGroup
            .addOnButtonCheckedListener {

                    _,
                    checkedId,
                    isChecked ->

                /*
                 * We only care about the button
                 * that became checked.
                 */
                if (!isChecked) {
                    return@addOnButtonCheckedListener
                }

                when (checkedId) {

                    R.id.monthlyButton -> {

                        viewModel.showMonthlySummary()
                    }

                    R.id.yearlyButton -> {

                        viewModel.showYearlySummary()
                    }

                    R.id.allTimeButton -> {

                        viewModel.showAllTimeSummary()
                    }
                }
            }
    }

    // ================================================================
    // Calendar
    // ================================================================

    private fun setupCalendar() {

        /*
         * Show today's date when Dashboard opens.
         */
        updateSelectedDateText()

        /*
         * The calendar is NOT permanently displayed.
         *
         * Tapping this button opens a modal DatePickerDialog.
         */
        binding.calendarButton.setOnClickListener {

            showCalendarDialog()
        }
    }

    private fun showCalendarDialog() {

        /*
         * Start the DatePicker with the currently
         * selected date.
         *
         * This means if the user selected September 20
         * before, opening the calendar again will start
         * on September 20.
         */
        val calendar =
            selectedDate.clone() as Calendar

        val dialog =
            DatePickerDialog(
                requireContext(),

                /*
                 * This callback runs when the user
                 * selects a date.
                 */
                { _, year, month, dayOfMonth ->

                    /*
                     * Update our selected date.
                     */
                    selectedDate =
                        Calendar.getInstance().apply {

                            set(
                                Calendar.YEAR,
                                year
                            )

                            set(
                                Calendar.MONTH,
                                month
                            )

                            set(
                                Calendar.DAY_OF_MONTH,
                                dayOfMonth
                            )

                            set(
                                Calendar.HOUR_OF_DAY,
                                0
                            )

                            set(
                                Calendar.MINUTE,
                                0
                            )

                            set(
                                Calendar.SECOND,
                                0
                            )

                            set(
                                Calendar.MILLISECOND,
                                0
                            )
                        }

                    /*
                     * Update the text at the top.
                     */
                    updateSelectedDateText()

                    /*
                     * Tell ViewModel to show
                     * transactions for this date.
                     */
                    viewModel.filterByDate(
                        year,
                        month,
                        dayOfMonth
                    )

                    /*
                     * DatePickerDialog closes automatically
                     * after selecting the date.
                     */
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
            )

        dialog.show()
    }

    private fun updateSelectedDateText() {

        val dateFormat =
            SimpleDateFormat(
                "MMMM d, yyyy",
                Locale.US
            )

        binding.selectedDateTextView.text =
            "Transactions for ${
                dateFormat.format(
                    selectedDate.time
                )
            }"
    }

    // ================================================================
    // Search
    // ================================================================

    private fun setupSearch() {

        binding.searchEditText
            .doAfterTextChanged { editable ->

                val query =
                    editable
                        ?.toString()
                        ?.trim()
                        ?: ""

                viewModel.searchTransactions(
                    query
                )
            }
    }

    // ================================================================
    // Income / Expense filter
    // ================================================================

    private fun setupTypeFilter() {

        /*
         * "All" is selected initially.
         */
        binding.typeRadioGroup.check(
            R.id.allRadioButton
        )

        binding.typeRadioGroup
            .setOnCheckedChangeListener {
                    _,
                    checkedId ->

                when (checkedId) {

                    R.id.allRadioButton -> {

                        viewModel.filterByType(
                            "all"
                        )
                    }

                    R.id.incomeRadioButton -> {

                        viewModel.filterByType(
                            "income"
                        )
                    }

                    R.id.expenseRadioButton -> {

                        viewModel.filterByType(
                            "expense"
                        )
                    }
                }
            }
    }

    // ================================================================
    // Clear filters
    // ================================================================

    private fun setupClearFilters() {

        binding.clearFiltersButton
            .setOnClickListener {

                /*
                 * Clear search.
                 */
                binding.searchEditText
                    .setText("")

                /*
                 * Select All.
                 */
                binding.typeRadioGroup.check(
                    R.id.allRadioButton
                )

                /*
                 * Return to today's date.
                 */
                selectedDate =
                    Calendar.getInstance().apply {

                        set(
                            Calendar.HOUR_OF_DAY,
                            0
                        )

                        set(
                            Calendar.MINUTE,
                            0
                        )

                        set(
                            Calendar.SECOND,
                            0
                        )

                        set(
                            Calendar.MILLISECOND,
                            0
                        )
                    }

                updateSelectedDateText()

                /*
                 * Reset all filters in ViewModel.
                 */
                viewModel.clearFilters()
            }
    }

    // ================================================================
    // Add Transaction
    // ================================================================

    private fun setupAddButton() {

        binding.addTransactionButton
            .setOnClickListener {

                findNavController().navigate(
                    R.id.action_dashboardFragment_to_addTransactionFragment
                )
            }
    }

    // ================================================================
    // Logout
    // ================================================================

    private fun setupLogout() {

        binding.logoutButton
            .setOnClickListener {

                /*
                 * Sign out from Firebase.
                 */
                authViewModel.logout()

                /*
                 * Go back to Login.
                 *
                 * We don't need a special
                 * dashboard -> login action.
                 */
                findNavController()
                    .popBackStack()
            }
    }

    // ================================================================
    // Observe ViewModel
    // ================================================================

    private fun observeUiState() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        viewModel.uiState.collect { state ->

                            /*
                             * Loading indicator.
                             */
                            binding.progressBar.visibility =
                                if (state.isLoading) {

                                    View.VISIBLE

                                } else {

                                    View.GONE
                                }

                            /*
                             * Income.
                             */
                            binding.incomeTextView.text =
                                formatMoney(
                                    state.totalIncomeCents
                                )

                            /*
                             * Expense.
                             */
                            binding.expenseTextView.text =
                                formatMoney(
                                    state.totalExpenseCents
                                )

                            /*
                             * Balance =
                             *
                             * income - expense
                             */
                            binding.balanceTextView.text =
                                formatMoney(
                                    state.totalIncomeCents -
                                            state.totalExpenseCents
                                )

                            /*
                             * Create quick date buttons
                             * for dates that contain transactions.
                             */
                            updateTransactionDateButtons(
                                state.transactionDates
                            )

                            /*
                             * Update RecyclerView.
                             */
                            transactionAdapter.submitList(
                                state.transactions
                            )

                            /*
                             * Show "No transactions found"
                             * when the filtered list is empty.
                             */
                            binding.emptyTextView.visibility =
                                if (
                                    state.transactions.isEmpty() &&
                                    !state.isLoading
                                ) {

                                    View.VISIBLE

                                } else {

                                    View.GONE
                                }

                            /*
                             * Show an error if Firestore
                             * returned an error.
                             */
                            state.errorMessage?.let { message ->

                                Toast.makeText(
                                    requireContext(),
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            }
    }

    // ================================================================
    // Money formatting
    // ================================================================

    private fun formatMoney(
        amountCents: Long
    ): String {

        return NumberFormat
            .getCurrencyInstance(Locale.US)
            .format(
                amountCents / 100.0
            )
    }

    // ================================================================
    // Delete dialog
    // ================================================================

    private fun showDeleteDialog(
        transactionId: String
    ) {

        androidx.appcompat.app.AlertDialog
            .Builder(requireContext())

            .setTitle(
                "Delete Transaction"
            )

            .setMessage(
                "Are you sure you want to delete this transaction?"
            )

            .setNegativeButton(
                "Cancel",
                null
            )

            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                viewModel.deleteTransaction(
                    transactionId
                )
            }

            .show()
    }

    // ================================================================
    // Quick date buttons
    // ================================================================

    private fun updateTransactionDateButtons(
        dates: List<Long>
    ) {

        /*
         * Remove old buttons first.
         */
        binding.transactionDatesContainer
            .removeAllViews()

        if (dates.isEmpty()) {
            return
        }

        val dateFormat =
            SimpleDateFormat(
                "MMM d",
                Locale.US
            )

        dates.forEach { dateMillis ->

            val button =
                MaterialButton(
                    requireContext()
                )

            button.text =
                dateFormat.format(
                    Date(dateMillis)
                )

            /*
             * Make the button smaller.
             */
            button.minHeight = 0

            button.minimumHeight = 0

            button.setPadding(
                20,
                0,
                20,
                0
            )

            button.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    48.dp()
                ).apply {

                    marginEnd =
                        8.dp()
                }

            button.setOnClickListener {

                /*
                 * Convert the button's date
                 * back into Calendar.
                 */
                val calendar =
                    Calendar.getInstance()

                calendar.timeInMillis =
                    dateMillis

                /*
                 * Update Dashboard selected date.
                 */
                selectedDate =
                    calendar.clone() as Calendar

                updateSelectedDateText()

                /*
                 * Update ViewModel filter.
                 */
                viewModel.filterByDate(

                    calendar.get(
                        Calendar.YEAR
                    ),

                    calendar.get(
                        Calendar.MONTH
                    ),

                    calendar.get(
                        Calendar.DAY_OF_MONTH
                    )
                )
            }

            binding.transactionDatesContainer
                .addView(button)
        }
    }

    // ================================================================
    // dp helper
    // ================================================================

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }

    // ================================================================
    // Reload transactions
    // ================================================================

    override fun onResume() {

        super.onResume()

        /*
         * Reload after returning from:
         *
         * Add Transaction
         * Edit Transaction
         * Delete Transaction
         */
        viewModel.loadTransactions()
    }

    // ================================================================
    // Destroy view
    // ================================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
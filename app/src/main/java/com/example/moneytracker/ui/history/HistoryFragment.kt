package com.example.moneytracker.ui.history

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentHistoryBinding
import com.example.moneytracker.ui.dashboard.MonthlyCalendarView
import com.example.moneytracker.ui.transaction.Money
import com.example.moneytracker.ui.transaction.TransactionAdapter
import com.example.moneytracker.ui.transaction.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.example.moneytracker.ui.transaction.TransactionCsv
import com.example.moneytracker.ui.transaction.TransactionFilters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {
    private val viewModel: TransactionViewModel by viewModels()
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private var calendarDialog: androidx.appcompat.app.AlertDialog? = null
    private var calendarView: MonthlyCalendarView? = null
    private lateinit var adapter: TransactionAdapter
    private var filtersExpanded = false
    private var pendingExportPath: String? = null
    private var preparingExport = false
    private var renderedFilterKey: List<Any?>? = null
    private val exportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val appContext = requireContext().applicationContext
        val file = pendingExportPath?.let { File(it) }
        pendingExportPath = null
        if (file == null || file.parentFile?.canonicalFile != appContext.cacheDir.canonicalFile ||
            !file.name.startsWith("moneytracker-export-")) return@registerForActivityResult
        if (uri == null) {
            file.delete()
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    requireNotNull(appContext.contentResolver.openOutputStream(uri, "wt")).use { output ->
                        file.inputStream().use { it.copyTo(output) }
                    }
                }
                true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                false
            } finally {
                file.delete()
            }
            _binding?.let {
                Snackbar.make(it.root, if (saved) R.string.export_success else R.string.export_failed, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.requestFocus()
        pendingExportPath = savedInstanceState?.getString("pendingExportPath") ?: pendingExportPath
        filtersExpanded = savedInstanceState?.getBoolean("filtersExpanded") ?: filtersExpanded
        binding.filtersPanel.visibility = if (filtersExpanded) View.VISIBLE else View.GONE
        binding.filtersButton.setOnClickListener {
            android.transition.TransitionManager.beginDelayedTransition(
                binding.root as android.view.ViewGroup,
                android.transition.AutoTransition().apply { duration = 160 })
            filtersExpanded = !filtersExpanded
            binding.filtersPanel.visibility = if (filtersExpanded) View.VISIBLE else View.GONE
        }
        binding.exportButton.setOnClickListener { exportHistory() }
        binding.totalsButton.setOnClickListener {
            val totals = TransactionFilters.totals(viewModel.uiState.value.transactions)
            val content = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val padding = (24 * resources.displayMetrics.density).toInt()
                setPadding(padding, 0, padding, padding)
                addView(android.widget.TextView(context).apply {
                    setText(R.string.filtered_totals_hint)
                    setTextColor(context.getColor(R.color.text_secondary))
                })
                totals.forEach { total ->
                    addView(android.widget.TextView(context).apply {
                        text = getString(R.string.month_totals, Money.format(total.income, total.currency),
                            Money.format(total.expense, total.currency), Money.format(total.income - total.expense, total.currency))
                        textSize = 16f
                        setTextColor(context.getColor(R.color.text_primary))
                        setPadding(0, padding, 0, 0)
                    })
                }
            }
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.filtered_totals_title)
                .setView(android.widget.ScrollView(requireContext()).apply { addView(content) })
                .setPositiveButton(R.string.close, null).show()
        }
        val categories = listOf<String?>(null) + resources.getStringArray(R.array.transaction_categories).toList()
        binding.categoryFilterSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            categories.map { it ?: getString(R.string.all_categories) }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.categoryFilterSpinner.setSelection(categories.indexOf(viewModel.uiState.value.categoryFilter).coerceAtLeast(0))
        binding.categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.filterByCategory(categories[position])
            }
        }
        val thisMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val months = listOf(null, thisMonth.timeInMillis,
            (thisMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }.timeInMillis)
        binding.monthFilterSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.month_filter_options)).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.monthFilterSpinner.setSelection(months.indexOf(viewModel.uiState.value.monthFilterMillis).coerceAtLeast(0))
        binding.monthFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (viewModel.uiState.value.monthFilterMillis != months[position]) viewModel.filterByMonth(months[position])
            }
        }
        binding.sortSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.sort_options)).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.sortSpinner.setSelection(TransactionFilters.sortOrders.indexOf(viewModel.uiState.value.sortOrder).coerceAtLeast(0))
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.sortBy(TransactionFilters.sortOrders[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        adapter = TransactionAdapter({ transaction ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_title).setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.text_delete) { _, _ -> viewModel.deleteTransaction(transaction.id) }
                .show()
        }, { transaction ->
            findNavController().navigate(R.id.editTransactionFragment,
                Bundle().apply { putString("transactionId", transaction.id) })
        })
        binding.transactionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionRecyclerView.adapter = adapter
        binding.searchEditText.setText(viewModel.uiState.value.searchQuery)
        binding.searchEditText.doAfterTextChanged { viewModel.searchTransactions(it?.toString().orEmpty()) }
        binding.typeToggleGroup.check(when (viewModel.selectedType()) {
            "income" -> R.id.incomeTypeButton
            "expense" -> R.id.expenseTypeButton
            else -> R.id.allTypeButton
        })
        binding.typeToggleGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked) viewModel.filterByType(when (id) {
                R.id.incomeTypeButton -> "income"
                R.id.expenseTypeButton -> "expense"
                else -> "all"
            })
        }
        binding.allDatesButton.setOnClickListener { viewModel.showAllDates() }
        binding.todayButton.setOnClickListener { selectDate(Calendar.getInstance()) }
        binding.calendarButton.setOnClickListener { showCalendar() }
        val currencies = listOf<String?>(null) + Money.currencies
        binding.currencySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            currencies.map { it ?: getString(R.string.all_currencies) }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.currencySpinner.setSelection(currencies.indexOf(viewModel.uiState.value.listCurrencyCode).coerceAtLeast(0))
        binding.currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.filterByCurrency(currencies[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.resetButton.setOnClickListener {
            viewModel.clearFilters()
            binding.searchEditText.setText("")
            binding.typeToggleGroup.check(R.id.allTypeButton)
            binding.currencySpinner.setSelection(0)
            binding.categoryFilterSpinner.setSelection(0)
            binding.monthFilterSpinner.setSelection(0)
        }
        binding.retryButton.setOnClickListener { viewModel.loadTransactions() }
        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.addTransactionFragment, Bundle().apply {
                putLong("selectedDate", if (viewModel.uiState.value.allDates) System.currentTimeMillis() else viewModel.selectedDateMillis())
                putString("currencyCode", viewModel.uiState.value.listCurrencyCode ?: com.example.moneytracker.data.repository.AppPreferences(requireContext()).currency())
            })
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.transactions)
                    val filterKey = listOf(state.sortOrder, state.searchQuery, state.listCurrencyCode,
                        state.allDates, if (state.allDates) null else viewModel.selectedDateMillis(), viewModel.selectedType(),
                        state.categoryFilter, state.monthFilterMillis)
                    if (renderedFilterKey != null && renderedFilterKey != filterKey) {
                        binding.transactionRecyclerView.scrollToPosition(0)
                    }
                    renderedFilterKey = filterKey
                    binding.exportButton.isEnabled = !state.isLoading && state.transactions.isNotEmpty() && !preparingExport
                    binding.totalsButton.isEnabled = !state.isLoading && state.transactions.isNotEmpty()
                    binding.allDatesButton.isChecked = state.allDates && state.monthFilterMillis == null
                    val monthIndex = months.indexOf(state.monthFilterMillis).coerceAtLeast(0)
                    if (binding.monthFilterSpinner.selectedItemPosition != monthIndex) binding.monthFilterSpinner.setSelection(monthIndex)
                    binding.calendarButton.text = if (state.allDates) getString(R.string.choose_date)
                        else SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(viewModel.selectedDateMillis())
                    val period = state.monthFilterMillis?.let { SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it) }
                        ?: if (!state.allDates) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(viewModel.selectedDateMillis()) else null
                    binding.countTextView.text = listOfNotNull(resources.getQuantityString(R.plurals.result_count,
                        state.transactions.size, state.transactions.size), period, state.categoryFilter, state.listCurrencyCode).joinToString(" · ")
                    val filtersActive = state.searchQuery.isNotBlank() || viewModel.selectedType() != "all" || !state.allDates || state.listCurrencyCode != null || state.categoryFilter != null || state.monthFilterMillis != null
                    val filterCount = listOf(viewModel.selectedType() != "all", !state.allDates || state.monthFilterMillis != null,
                        state.listCurrencyCode != null, state.categoryFilter != null).count { it }
                    binding.filtersButton.text = if (filterCount == 0) getString(R.string.filters) else getString(R.string.filters_count, filterCount)
                    binding.emptyTextView.setText(if (filtersActive) R.string.history_no_matches else R.string.history_empty)
                    binding.emptyTextView.visibility = if (state.transactions.isEmpty() && !state.isLoading && state.errorMessage == null) View.VISIBLE else View.GONE
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.errorTextView.text = state.errorMessage
                    binding.errorTextView.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
                    binding.retryButton.visibility = binding.errorTextView.visibility
                    binding.retryButton.isEnabled = !state.isLoading
                    calendarView?.setTransactionDates(state.transactionDates)
                }
            }
        }
    }

    private fun selectDate(date: Calendar) {
        viewModel.filterByDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
    }

    private fun exportHistory() {
        if (preparingExport || viewModel.uiState.value.transactions.isEmpty()) return
        preparingExport = true
        binding.exportButton.isEnabled = false
        val transactions = viewModel.uiState.value.transactions.toList()
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    File.createTempFile("moneytracker-export-", ".csv", appContext.cacheDir).apply {
                        writeText("\uFEFF" + TransactionCsv.export(transactions), Charsets.UTF_8)
                    }
                }
                pendingExportPath = file.absolutePath
                exportDocument.launch("MoneyTracker-${java.time.LocalDate.now()}.csv")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Snackbar.make(binding.root, R.string.export_failed, Snackbar.LENGTH_LONG).show()
            } finally {
                preparingExport = false
                _binding?.exportButton?.isEnabled = true
            }
        }
    }

    private fun showCalendar() {
        val date = Calendar.getInstance().apply { timeInMillis = viewModel.selectedDateMillis() }
        val calendar = MonthlyCalendarView(requireContext()).apply {
            setMonth(date)
            setSelectedDate(date)
            setTransactionDates(viewModel.uiState.value.transactionDates)
        }
        calendarView = calendar
        val content = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 0, 16, 16)
            addView(calendar)
            addView(android.widget.TextView(requireContext()).apply {
                setText(R.string.calendar_transaction_legend)
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setPadding(16, 8, 16, 8)
            })
        }
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.select_transaction_date)
            .setView(content).setNegativeButton(R.string.cancel, null).create()
        calendarDialog = dialog
        calendar.setOnDateSelectedListener { selectDate(it); dialog.dismiss() }
        dialog.setOnDismissListener { calendarView = null; calendarDialog = null }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("successMessage")?.let {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        }
        viewModel.loadTransactions()
    }

    override fun onDestroyView() {
        calendarDialog?.dismiss()
        binding.transactionRecyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("filtersExpanded", filtersExpanded)
        outState.putString("pendingExportPath", pendingExportPath)
    }
}

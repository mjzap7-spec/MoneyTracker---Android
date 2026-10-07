package com.example.moneytracker.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentSettingsBinding
import com.example.moneytracker.data.repository.AppPreferences
import com.example.moneytracker.ui.transaction.CurrencyPicker
import com.example.moneytracker.ui.transaction.Money
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentSettingsBinding.bind(view)
        binding.root.requestFocus()
        binding.logoutButton.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sign_out_question)
                .setMessage(R.string.sign_out_hint)
                .setNegativeButton(R.string.stay_signed_in, null)
                .setPositiveButton(R.string.sign_out) { _, _ ->
                    com.example.moneytracker.data.repository.AuthRepository().logout()
                    val nav = androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    listOf(R.id.historyFragment, R.id.insightsFragment, R.id.settingsFragment, R.id.dashboardFragment)
                        .forEach { nav.clearBackStack(it) }
                    nav.navigate(R.id.action_logout)
                }.show()
        }
        binding.helpButton.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.helpFragment)
        }
        val preferences = AppPreferences(requireContext())
        CurrencyPicker.setup(binding.currencySpinner, preferences.currency()) { preferences.setCurrency(it) }
        binding.manageBudgetsButton.setOnClickListener {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(R.id.budgetsFragment)
        }
        val themes = listOf("dark", "light", "system")
        val types = listOf("expense", "income")
        binding.defaultTypeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.default_type_options)).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.defaultTypeSpinner.setSelection(types.indexOf(preferences.transactionType()))
        binding.defaultTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                preferences.setTransactionType(types[position])
            }
        }
        binding.themeSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.theme_options)).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.themeSpinner.setSelection(themes.indexOf(preferences.theme()).coerceAtLeast(0))
        binding.themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val theme = themes[position]
                if (theme != preferences.theme()) {
                    preferences.setTheme(theme)
                    AppCompatDelegate.setDefaultNightMode(themeMode(theme))
                }
            }
        }
    }
    companion object {
        fun themeMode(theme: String) = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
    }
}

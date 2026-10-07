package com.example.moneytracker.ui.transaction

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.util.Currency

object CurrencyPicker {
    fun setup(spinner: Spinner, code: String = "USD", showNames: Boolean = true, onSelected: (String) -> Unit) {
        val labels = Money.currencies.map {
            if (showNames) "$it — ${Currency.getInstance(it).displayName}" else it
        }
        spinner.adapter = ArrayAdapter(spinner.context, android.R.layout.simple_spinner_item, labels)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinner.setSelection(Money.currencies.indexOf(code).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected(Money.currencies[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    fun selected(spinner: Spinner): String = Money.currencies[spinner.selectedItemPosition.coerceAtLeast(0)]
}

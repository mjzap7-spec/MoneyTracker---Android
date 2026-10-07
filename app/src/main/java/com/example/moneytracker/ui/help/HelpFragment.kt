package com.example.moneytracker.ui.help

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moneytracker.R
import com.example.moneytracker.databinding.FragmentHelpBinding
import com.google.android.material.button.MaterialButton

class HelpFragment : Fragment(R.layout.fragment_help) {
    private val expanded = mutableSetOf<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentHelpBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        expanded.clear()
        expanded.addAll(savedInstanceState?.getIntArray("expanded")?.toList() ?: listOf(0))
        val titles = resources.getStringArray(R.array.help_questions)
        val answers = resources.getStringArray(R.array.help_answers)
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        titles.forEachIndexed { index, title ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setBackgroundResource(R.drawable.bg_section_card)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            }
            val question = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = title
                isAllCaps = false
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                strokeWidth = 0
                minHeight = dp(56)
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            }
            val answer = TextView(requireContext()).apply {
                text = answers[index]
                textSize = 15f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                setPadding(dp(12), dp(4), dp(12), dp(16))
                setLineSpacing(dp(4).toFloat(), 1f)
            }
            fun render() {
                val open = index in expanded
                answer.visibility = if (open) View.VISIBLE else View.GONE
                ViewCompat.setStateDescription(question, getString(if (open) R.string.help_expanded else R.string.help_collapsed))
                question.setIconResource(if (open) R.drawable.ic_help_collapse else R.drawable.ic_expand_more)
                question.iconGravity = MaterialButton.ICON_GRAVITY_END
                question.iconTint = ContextCompat.getColorStateList(requireContext(), R.color.text_secondary)
            }
            question.setOnClickListener {
                if (!expanded.add(index)) expanded.remove(index)
                render()
            }
            card.addView(question)
            card.addView(answer)
            binding.topics.addView(card)
            render()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray("expanded", expanded.toIntArray())
        super.onSaveInstanceState(outState)
    }
}

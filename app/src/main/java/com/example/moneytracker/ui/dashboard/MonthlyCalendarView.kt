package com.example.moneytracker.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.moneytracker.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MonthlyCalendarView(
    context: Context
) : LinearLayout(context) {

    private var currentMonth =
        Calendar.getInstance()

    private var selectedDate =
        Calendar.getInstance()

    private var onDateSelected:
            ((Calendar) -> Unit)? = null

    private var transactionDays = emptySet<Pair<Int, Int>>()

    fun setTransactionDates(dates: List<Long>) {
        val days = dates.map { millis ->
            Calendar.getInstance().apply { timeInMillis = millis }.let {
                it.get(Calendar.YEAR) to it.get(Calendar.DAY_OF_YEAR)
            }
        }.toSet()
        if (days != transactionDays) {
            transactionDays = days
            buildCalendar()
        }
    }

    init {

        orientation =
            VERTICAL

        currentMonth.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        buildCalendar()
    }

    fun setMonth(
        calendar: Calendar
    ) {

        currentMonth =
            calendar.clone() as Calendar

        currentMonth.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        buildCalendar()
    }

    fun setSelectedDate(
        calendar: Calendar
    ) {

        selectedDate =
            calendar.clone() as Calendar

        selectedDate.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        selectedDate.set(
            Calendar.MINUTE,
            0
        )

        selectedDate.set(
            Calendar.SECOND,
            0
        )

        selectedDate.set(
            Calendar.MILLISECOND,
            0
        )

        buildCalendar()
    }

    fun setOnDateSelectedListener(
        listener: (Calendar) -> Unit
    ) {

        onDateSelected =
            listener
    }

    private fun buildCalendar() {

        removeAllViews()

        addMonthHeader()
        addWeekHeader()

        val firstDay =
            currentMonth.clone() as Calendar

        firstDay.set(
            Calendar.DAY_OF_MONTH,
            1
        )

        val firstDayOfWeek =
            firstDay.get(
                Calendar.DAY_OF_WEEK
            )

        val startingPosition =
            firstDayOfWeek - 1

        val daysInMonth =
            currentMonth.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        var day = 1

        var position = 0

        while (day <= daysInMonth) {

            val weekRow =
                LinearLayout(context)

            weekRow.orientation =
                HORIZONTAL

            weekRow.gravity =
                Gravity.CENTER_VERTICAL

            weekRow.layoutParams =
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    58.dp()
                )

            for (column in 0 until 7) {

                if (
                    position < startingPosition ||
                    day > daysInMonth
                ) {

                    addEmptyDay(
                        weekRow
                    )

                } else {

                    addDay(
                        weekRow,
                        day
                    )

                    day++
                }

                position++
            }

            addView(weekRow)
        }
    }

    private fun addMonthHeader() {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 56.dp())
        }
        fun navigationButton(label: String, description: String, offset: Int) =
            TextView(context).apply {
                text = label
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(context.getColor(R.color.primary_blue))
                contentDescription = description
                layoutParams = LayoutParams(48.dp(), 48.dp())
                isFocusable = true
                setOnClickListener {
                    currentMonth.add(Calendar.MONTH, offset)
                    buildCalendar()
                }
            }
        row.addView(navigationButton("‹", context.getString(R.string.previous_month), -1))
        row.addView(TextView(context).apply {
            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(context.getColor(R.color.text_primary))
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(navigationButton("›", context.getString(R.string.next_month), 1))
        addView(row)
    }

    private fun addWeekHeader() {

        val row =
            LinearLayout(context)

        row.orientation =
            HORIZONTAL

        row.gravity =
            Gravity.CENTER

        row.layoutParams =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                32.dp()
            )

        val days =
            listOf(
                "S",
                "M",
                "T",
                "W",
                "T",
                "F",
                "S"
            )

        days.forEach { day ->

            val textView =
                TextView(context)

            textView.text =
                day

            textView.gravity =
                Gravity.CENTER

            textView.textSize =
                12f

            textView.setTextColor(
                context.getColor(
                    R.color.text_muted
                )
            )

            textView.layoutParams =
                LayoutParams(
                    0,
                    LayoutParams.MATCH_PARENT,
                    1f
                )

            row.addView(
                textView
            )
        }

        addView(row)
    }

    private fun addEmptyDay(
        row: LinearLayout
    ) {

        val emptyView =
            TextView(context)

        emptyView.layoutParams =
            LayoutParams(
                0,
                LayoutParams.MATCH_PARENT,
                1f
            )

        row.addView(
            emptyView
        )
    }

    private fun addDay(
        row: LinearLayout,
        day: Int
    ) {

        val container =
            LinearLayout(context)

        container.orientation =
            VERTICAL

        container.gravity =
            Gravity.CENTER

        container.layoutParams =
            LayoutParams(
                0,
                LayoutParams.MATCH_PARENT,
                1f
            )

        val dayText =
            TextView(context)

        dayText.text =
            day.toString()

        dayText.gravity =
            Gravity.CENTER

        dayText.textSize =
            14f

        dayText.setTextColor(
            context.getColor(
                R.color.text_primary
            )
        )

        val size =
            36.dp()

        dayText.layoutParams =
            LayoutParams(
                size,
                size
            )

        val calendar =
            currentMonth.clone() as Calendar

        calendar.set(
            Calendar.DAY_OF_MONTH,
            day
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        if (
            isSameDay(
                calendar,
                selectedDate
            )
        ) {

            dayText.background =
                createCircleBackground(
                    R.color.primary_blue
                )

            dayText.setTextColor(
                Color.WHITE
            )
        }

        container.addView(
            dayText
        )

        val hasTransactions = (calendar.get(Calendar.YEAR) to
            calendar.get(Calendar.DAY_OF_YEAR)) in transactionDays
        container.addView(View(context).apply {
            layoutParams = LayoutParams(5.dp(), 5.dp()).apply { topMargin = 3.dp() }
            background = createCircleBackground(R.color.income_green)
            visibility = if (hasTransactions) View.VISIBLE else View.INVISIBLE
        })
        val dateLabel = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            .format(calendar.time)
        container.contentDescription = if (hasTransactions)
            context.getString(R.string.calendar_date_with_transactions, dateLabel) else dateLabel
        container.isFocusable = true

        container.setOnClickListener {

            selectedDate =
                calendar.clone()
                        as Calendar

            buildCalendar()

            onDateSelected?.invoke(
                selectedDate.clone()
                        as Calendar
            )
        }

        row.addView(
            container
        )
    }

    private fun isSameDay(
        first: Calendar,
        second: Calendar
    ): Boolean {

        return first.get(
            Calendar.YEAR
        ) ==
                second.get(
                    Calendar.YEAR
                ) &&
                first.get(
                    Calendar.DAY_OF_YEAR
                ) ==
                second.get(
                    Calendar.DAY_OF_YEAR
                )
    }

    private fun createCircleBackground(
        colorResId: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape =
                GradientDrawable.OVAL

            setColor(
                context.getColor(
                    colorResId
                )
            )
        }
    }

    private fun Int.dp(): Int {

        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }
}

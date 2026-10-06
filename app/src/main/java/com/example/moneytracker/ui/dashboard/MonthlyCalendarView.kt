package com.example.moneytracker.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
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
package com.example.moneytracker

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.moneytracker.ui.assistant.TransactionDraft
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantDraftReviewTest {
    @Test fun validatedDraftPrefillsFormWithoutSaving() {
        val draft = TransactionDraft.validated("15.25", "EUR", "income", "Salary", "Test draft only",
            "2026-10-05", "")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val navigation = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
                navigation.navigate(R.id.addTransactionFragment, Bundle().apply {
                    putLong("draftAmount", draft.amountCents); putString("draftType", draft.type)
                    putString("draftCategory", draft.category); putString("draftNote", draft.note)
                    putString("currencyCode", draft.currency); putLong("selectedDate", draft.dateMillis)
                })
            }
            onView(withId(R.id.amountEditText)).check(matches(withText("15.25")))
            onView(withId(R.id.currencySpinner)).check(matches(withSpinnerText(containsString("EUR"))))
            onView(withId(R.id.categorySpinner)).check(matches(withSpinnerText("Salary")))
            onView(withId(R.id.incomeRadioButton)).check(matches(isChecked()))
            onView(withId(R.id.descriptionEditText)).check(matches(withText("Test draft only")))
        }
    }
}

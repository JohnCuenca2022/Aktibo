package com.example.aktibo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

class LoginActivityTest {

    @get:Rule
    var activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun test_isActivityInView() {
        onView(withId(R.id.main))
            .check(matches(isDisplayed()))
    }
}

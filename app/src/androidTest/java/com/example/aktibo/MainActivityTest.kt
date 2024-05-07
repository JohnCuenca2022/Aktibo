package com.example.aktibo

import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`


class MainActivityTest {

    @get:Rule
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun test_isActivityInView() {
        onView(withId(R.id.main))
            .check(matches(isDisplayed()))
    }
    @Test
    fun test_isFragmentContainerInView(){
        onView(withId(R.id.fragment_container))
            .check(matches(isDisplayed()))
    }
    @Test
    fun test_canNavigatetoHome(){
        onView(allOf(withId(R.id.menu_home), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(withId(R.id.homeFragment))
            .check(matches(isDisplayed()))

    }

    @Test
    fun test_canNavigatetoAccountFragment(){
        onView(allOf(withId(R.id.menu_home), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(allOf(withId(R.id.imageButtonAccount), isDescendantOfA(withId(R.id.homeFragment)))).perform(click())
        onView(withId(R.id.accountFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_canNavigatetoExerciseGoalFragment(){
        onView(allOf(withId(R.id.menu_exercise), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(allOf(withId(R.id.exerciseGoalButton), isDescendantOfA(withId(R.id.exerciseFragment)))).perform(click())
        onView(withId(R.id.exerciseGoalFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_canNavigatetoFoodRecordFragment(){
        onView(allOf(withId(R.id.menu_food), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(allOf(withId(R.id.foodRecordButton), isDescendantOfA(withId(R.id.foodFragment)))).perform(click())
        onView(withId(R.id.foodRecordFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_canNavigatetoMealRecipesFragment(){
        onView(allOf(withId(R.id.menu_food), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(allOf(withId(R.id.mealRecipesButton), isDescendantOfA(withId(R.id.foodFragment)))).perform(click())
        onView(withId(R.id.mealRecipesFragment))
            .check(matches(isDisplayed()))
    }

    @Test
    fun test_canNavigatetoFood(){
        onView(allOf(withId(R.id.menu_food), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(withId(R.id.foodFragment))
            .check(matches(isDisplayed()))

    }

    @Test
    fun test_canNavigatetoExercise(){
        onView(allOf(withId(R.id.menu_exercise), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(withId(R.id.exerciseFragment))
            .check(matches(isDisplayed()))

    }

    @Test
    fun test_canNavigatetoMoments(){
        onView(allOf(withId(R.id.menu_moments), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(withId(R.id.momentsFragment))
            .check(matches(isDisplayed()))

    }

    @Test
    fun test_canNavigatetoNotification(){
        onView(allOf(withId(R.id.menu_notifications), isDescendantOfA(withId(R.id.bottom_navigation)))).perform(click())
        onView(withId(R.id.notifFragment))
            .check(matches(isDisplayed()))

    }
}


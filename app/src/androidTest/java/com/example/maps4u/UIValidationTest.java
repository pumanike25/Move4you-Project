package com.example.maps4u;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UIValidationTest {

    // Tell the test to open ProfileActivity
    @Rule
    public ActivityScenarioRule<ProfileActivity> activityRule =
            new ActivityScenarioRule<>(ProfileActivity.class);

    @Test
    public void testTabsAreVisible() {
        // Find the TabLayout element on the screen and check if it's visible
        Espresso.onView(ViewMatchers.withId(R.id.tabLayout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testEditBiometricsButtonIsClickable() {
        // Find the button and check if it's displayed and clickable
        Espresso.onView(ViewMatchers.withId(R.id.btnEditBiometrics))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()));
    }
}
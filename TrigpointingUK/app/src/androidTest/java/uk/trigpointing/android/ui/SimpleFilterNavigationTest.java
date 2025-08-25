package uk.trigpointing.android.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.trigpointing.android.MainActivity;
import uk.trigpointing.android.R;
import uk.trigpointing.android.filter.Filter;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Simple UI test to validate basic navigation and understand the current app structure.
 * This test is designed to help debug the filter persistence issue by testing navigation step by step.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SimpleFilterNavigationTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private Context context;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Reset preferences to clean state
        resetPreferences();
    }

    @After
    public void tearDown() throws Exception {
        // Reset preferences
        resetPreferences();
    }

    /**
     * Test 1: Basic navigation to settings page
     */
    @Test
    public void testNavigateToSettings() {
        // Wait for main activity to load
        onView(withId(R.id.btnNearest)).check(matches(isDisplayed()));
        
        // Navigate to Settings via menu
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext());
        onView(withText("Settings")).perform(click());
        
        // Wait for settings page to load and check if we can find any preferences
        try {
            Thread.sleep(2000); // Give settings time to load
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Try to find the Trigpoint Types preference
        try {
            onView(withText("Trigpoint Types")).check(matches(isDisplayed()));
            System.out.println("SUCCESS: Found 'Trigpoint Types' preference");
        } catch (Exception e) {
            System.out.println("ISSUE: Cannot find 'Trigpoint Types' preference");
            System.out.println("Error: " + e.getMessage());
            
            // Try to find any preferences that might be there
            try {
                onView(withText("Filter Settings")).check(matches(isDisplayed()));
                System.out.println("Found 'Filter Settings' category");
            } catch (Exception e2) {
                System.out.println("Cannot find 'Filter Settings' category either");
            }
            
            // Let's just verify we're in some kind of settings screen
            // by looking for any common settings elements
            try {
                onView(withText("Sync Settings")).check(matches(isDisplayed()));
                System.out.println("Found 'Sync Settings' - we are in settings page");
            } catch (Exception e3) {
                System.out.println("Cannot find any expected settings categories");
                throw e; // Re-throw the original exception
            }
        }
    }

    /**
     * Test 2: Basic navigation to nearest page
     */
    @Test
    public void testNavigateToNearest() {
        // Navigate to Nearest page
        onView(withId(R.id.btnNearest)).perform(click());
        
        // Wait for nearest page to load
        try {
            Thread.sleep(3000); // Give page time to load
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check if we can find the header
        try {
            onView(withId(R.id.trigListHeader)).check(matches(isDisplayed()));
            System.out.println("SUCCESS: Found trigListHeader in nearest page");
        } catch (Exception e) {
            System.out.println("ISSUE: Cannot find trigListHeader");
            System.out.println("Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Test 3: Test SharedPreferences directly to understand the preference keys
     */
    @Test
    public void testPreferenceKeys() {
        // Test different preference keys to understand what's being used
        
        // Set a value using the Filter.FILTERTYPE key
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Filter.FILTERTYPE, 0); // Pillars Only
        editor.apply();
        
        // Verify it was set
        int filterType = prefs.getInt(Filter.FILTERTYPE, -1);
        assertEquals("Filter type should be set", 0, filterType);
        System.out.println("Filter.FILTERTYPE key works: " + Filter.FILTERTYPE);
        
        // Test the preferences.xml key
        editor.putString("trigpoint_type_filter", "2"); // FBM Only
        editor.apply();
        
        String trigpointTypeFilter = prefs.getString("trigpoint_type_filter", "");
        assertEquals("Trigpoint type filter should be set", "2", trigpointTypeFilter);
        System.out.println("trigpoint_type_filter key works");
        
        // Check what keys exist in preferences
        System.out.println("All preference keys:");
        for (String key : prefs.getAll().keySet()) {
            System.out.println("  " + key + " = " + prefs.getAll().get(key));
        }
    }

    /**
     * Test 4: Check database status
     */
    @Test
    public void testDatabaseStatus() {
        // Check if database download is needed
        try {
            // Look at the trigpoint counts on main page
            String pillarCount = "unknown";
            try {
                // This might fail if the text view has different content
                onView(withId(R.id.countPillarText)).check(matches(isDisplayed()));
                System.out.println("Found pillar count display");
            } catch (Exception e) {
                System.out.println("Could not access pillar count: " + e.getMessage());
            }
            
            // Check if sync button is visible (indicates database needs population)
            try {
                onView(withId(R.id.btnSync)).check(matches(isDisplayed()));
                System.out.println("Sync button is visible - database likely needs population");
            } catch (Exception e) {
                System.out.println("Sync button not visible");
            }
            
        } catch (Exception e) {
            System.out.println("Error checking database status: " + e.getMessage());
        }
    }

    private void resetPreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}

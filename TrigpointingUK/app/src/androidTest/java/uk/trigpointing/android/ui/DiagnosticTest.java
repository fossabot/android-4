package uk.trigpointing.android.ui;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.trigpointing.android.MainActivity;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/**
 * Diagnostic test to understand what's actually displayed in the app
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DiagnosticTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testWhatIsDisplayed() {
        // Wait for app to load
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Try to find the title to confirm we're in the right app
        try {
            onView(withText("TrigpointingUK")).check(matches(isDisplayed()));
            System.out.println("Found app title 'TrigpointingUK'");
        } catch (Exception e) {
            System.out.println("Could not find app title: " + e.getMessage());
        }
        
        // Check if we're in some kind of intro/setup flow
        try {
            onView(withText("Download")).check(matches(isDisplayed()));
            System.out.println("Found 'Download' text - may be in setup flow");
        } catch (Exception e) {
            System.out.println("No 'Download' text found");
        }
        
        // Check if we're being asked to download trigpoints
        try {
            onView(withText("Sync")).check(matches(isDisplayed()));
            System.out.println("Found 'Sync' text - may need to download data");
        } catch (Exception e) {
            System.out.println("No 'Sync' text found");
        }
        
        // Check various possible button IDs
        String[] possibleTexts = {
            "Nearest", "Map", "Settings", "Download", "Sync", "Continue", "Next", "OK", "Start"
        };
        
        for (String text : possibleTexts) {
            try {
                onView(withText(text)).check(matches(isDisplayed()));
                System.out.println("Found button/text: '" + text + "'");
            } catch (Exception e) {
                // Ignore - not found
            }
        }
        
        // The test should always pass - it's just for diagnosis
        assert true;
    }
}

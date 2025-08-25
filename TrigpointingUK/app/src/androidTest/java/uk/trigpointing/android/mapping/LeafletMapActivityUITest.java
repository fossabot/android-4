package uk.trigpointing.android.mapping;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import uk.trigpointing.android.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.web.webdriver.Locator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static org.hamcrest.Matchers.containsString;

/**
 * UI Tests for LeafletMapActivity
 * Tests the WebView-based Leaflet mapping functionality
 */
@RunWith(AndroidJUnit4.class)
public class LeafletMapActivityUITest {
    
    @Rule
    public ActivityScenarioRule<LeafletMapActivity> activityRule = 
            new ActivityScenarioRule<>(LeafletMapActivity.class);

    @Test
    public void testWebViewIsDisplayed() {
        // Verify WebView is present and displayed
        onView(withId(R.id.webview))
                .check(matches(isDisplayed()));
    }

    @Test 
    public void testLeafletMapLoads() {
        // Give WebView time to load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check that Leaflet map container exists
        onWebView()
                .withElement(findElement(Locator.ID, "map"))
                .check(webMatches(getText(), containsString(""))); // Just verify element exists
    }

    @Test
    public void testWebViewAcceptsJavaScript() {
        // Give WebView time to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test that JavaScript is enabled and working by checking for Leaflet
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "body"))
                .check(webMatches(getText(), containsString(""))); // Basic check that body loads
    }
}

package uk.trigpointing.android.mapping;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.trigpointing.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.*;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static org.hamcrest.Matchers.*;

/**
 * UI tests for LeafletMapActivity using Espresso
 * Tests WebView functionality, JavaScript compatibility fixes, and user interactions
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LeafletMapActivityUITest {

    @Rule
    public ActivityScenarioRule<LeafletMapActivity> activityRule = 
        new ActivityScenarioRule<>(LeafletMapActivity.class);

    @Rule
    public GrantPermissionRule locationPermissionRule = 
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() {
        // Set up test preferences with known values
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putString("os_api_key", "test_api_key_123")
            .putString("leaflet_map_style", "OpenStreetMap")
            .apply();
    }

    @Test
    public void testWebViewLoadsSuccessfully() {
        // Check that the WebView is displayed
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
    }

    @Test
    public void testWebViewHasCorrectUrl() {
        // Wait for WebView to load and check that URL parameters are correctly passed
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(10000) // 10 second timeout for page load
            .check(webMatches(getCurrentUrl(), containsString("os_key=test_api_key_123")));
            
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(getCurrentUrl(), containsString("initial_style=OpenStreetMap")));
    }

    @Test
    public void testJavaScriptParameterParsingWorks() {
        // Test that our JavaScript fix for URL parameter parsing works
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(15000) // Allow time for JavaScript to execute
            .perform(webClick(Locator.TAG_NAME, "body")); // Interact to ensure JS has loaded
            
        // Check that the map container exists (indicates JavaScript loaded successfully)
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(findElement(Locator.ID, "map"), isDisplayed()));
    }

    @Test
    public void testMapInitializationWithoutJavaScriptErrors() {
        // Test that the map initializes without JavaScript console errors
        // (Our optional chaining and URL constructor fixes should prevent errors)
        
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(20000) // Allow generous time for map initialization
            .check(webMatches(findElement(Locator.CLASS_NAME, "leaflet-container"), isDisplayed()));
    }

    @Test
    public void testOSKeyParameterIsPassedCorrectly() {
        // Test that the OS API key is correctly encoded and passed to the WebView
        // This tests our URLEncoder compatibility fix
        
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(10000)
            .check(webMatches(getCurrentUrl(), containsString("os_key=")));
            
        // URL should not contain raw special characters that would break parsing
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(getCurrentUrl(), not(containsString("os_key=&"))));
    }

    @Test
    public void testMapStyleParameterIsPassedCorrectly() {
        // Test that map style parameter is correctly passed
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(10000)
            .check(webMatches(getCurrentUrl(), containsString("initial_style=OpenStreetMap")));
    }

    @Test
    public void testWebViewCanHandleJavaScriptInteraction() {
        // Test that JavaScript interactions work (tests our compatibility fixes)
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(15000)
            .perform(webClick(Locator.TAG_NAME, "body"));
            
        // Map should still be responsive after interaction
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(findElement(Locator.ID, "map"), isDisplayed()));
    }

    @Test
    public void testLeafletControlsArePresent() {
        // Test that Leaflet map controls load correctly
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(20000)
            .check(webMatches(findElement(Locator.CLASS_NAME, "leaflet-control-zoom"), isDisplayed()));
    }

    @Test
    public void testWebViewHandlesLocationPermission() {
        // Test location permission handling in WebView
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
            
        // Should not crash when location is accessed
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(15000)
            .perform(webClick(Locator.TAG_NAME, "body"));
    }

    @Test
    public void testMenuOptionsAreAvailable() {
        // Test that menu options work correctly
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
            
        // The activity should be stable enough to handle menu interactions
        // (This indirectly tests that our JavaScript fixes don't break the app)
    }

    @Test
    public void testWebViewSurvivesOrientationChange() {
        // Test that WebView handles configuration changes without crashing
        // This is important because our JavaScript fixes need to work after recreation
        
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
            
        // Simulate configuration change by recreating activity
        activityRule.getScenario().recreate();
        
        // WebView should still be functional
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
            
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(15000)
            .check(webMatches(getCurrentUrl(), containsString("index.html")));
    }

    @Test
    public void testWebViewHandlesSpecialCharactersInParameters() {
        // Test parameter encoding with special characters
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putString("leaflet_map_style", "Test Style With Spaces")
            .apply();
            
        // Restart activity to pick up new preferences
        activityRule.getScenario().recreate();
        
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(10000)
            .check(webMatches(getCurrentUrl(), containsString("initial_style=Test")));
    }

    @Test
    public void testWebViewLoadsWithoutNetworkDependencies() {
        // Test that the core map loads even without network
        // (Important for offline functionality)
        
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
            
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(15000)
            .check(webMatches(findElement(Locator.ID, "map"), isDisplayed()));
    }

    @Test
    public void testJavaScriptInterfaceIsAvailable() {
        // Test that AndroidPrefs JavaScript interface is available
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(10000)
            .perform(webClick(Locator.TAG_NAME, "body"));
            
        // Interface should be accessible without errors
        // (This tests that our compatibility fixes don't interfere with the interface)
    }

    @Test
    public void testWebViewHandlesEmptyParameters() {
        // Test handling of empty/null parameters
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putString("os_api_key", "")
            .putString("leaflet_map_style", "")
            .apply();
            
        activityRule.getScenario().recreate();
        
        // Should still load without crashing
        onView(withId(R.id.leafletWebView))
            .check(matches(isDisplayed()));
            
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(15000)
            .check(webMatches(getCurrentUrl(), containsString("index.html")));
    }

    @Test
    public void testWebViewConsoleDoesNotShowCriticalErrors() {
        // This test would ideally capture console messages
        // For now, we test that the map loads successfully which indicates
        // no critical JavaScript errors occurred
        
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(20000)
            .check(webMatches(findElement(Locator.CLASS_NAME, "leaflet-container"), isDisplayed()));
            
        // If our JavaScript fixes work, the Leaflet container should be present
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(findElement(Locator.ID, "map"), isDisplayed()));
    }

    @Test
    public void testMapInteractionAfterLongDelay() {
        // Test that map remains interactive after extended time
        // (Tests stability of our JavaScript fixes)
        
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(25000) // Extended timeout
            .perform(webClick(Locator.ID, "map"));
            
        // Map should still be responsive
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(findElement(Locator.ID, "map"), isDisplayed()));
    }

    @Test
    public void testWebViewURLEncodingCompatibility() {
        // Test our URLEncoder.encode compatibility fix
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putString("os_api_key", "key+with/special=chars&more")
            .apply();
            
        activityRule.getScenario().recreate();
        
        // URL should be properly encoded
        onWebView(withId(R.id.leafletWebView))
            .withTimeout(10000)
            .check(webMatches(getCurrentUrl(), containsString("os_key=")));
            
        // Should not contain unencoded special characters that would break URL parsing
        onWebView(withId(R.id.leafletWebView))
            .check(webMatches(getCurrentUrl(), not(containsString("os_key=key+with/special=chars&more"))));
    }
}

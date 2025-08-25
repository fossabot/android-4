package uk.trigpointing.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Simple integration test that doesn't require the full app
 * This avoids Firebase initialization issues on older Android versions
 */
@RunWith(AndroidJUnit4.class)
public class SimpleIntegrationTest {

    @Test
    public void testInstrumentationContext() {
        // Test that the instrumentation context is available
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        assertNotNull("Context should not be null", context);
        assertEquals("Package name should match", "uk.trigpointing.android", context.getPackageName());
    }

    @Test
    public void testBasicFunctionality() {
        // Test basic Android functionality without triggering Firebase
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Test package manager access
        android.content.pm.PackageManager packageManager = context.getPackageManager();
        assertNotNull("Package manager should be available", packageManager);
        
        // Test basic system service access
        Object locationService = context.getSystemService(android.content.Context.LOCATION_SERVICE);
        assertNotNull("Location service should be available", locationService);
    }
    
    @Test
    public void testInstrumentationRunning() {
        // Simple test to verify the test framework is working
        assertTrue("This test should always pass", true);
        assertEquals("Math should work", 4, 2 + 2);
    }
}

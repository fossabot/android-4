package uk.trigpointing.android.test;

import android.app.Application;
import android.util.Log;

/**
 * Test application that disables Firebase for integration tests
 * This prevents Firebase crashes on older Android versions during testing
 */
public class TestApplication extends Application {
    private static final String TAG = "TestApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "TestApplication started - Firebase disabled for testing");
        
        // Disable Firebase initialization during tests by setting system properties
        System.setProperty("firebase.test.lab", "true");
        System.setProperty("firebase.crash.collection_enabled", "false");
        System.setProperty("firebase.analytics.collection_enabled", "false");
        System.setProperty("firebase.perf.collection_enabled", "false");
    }
}

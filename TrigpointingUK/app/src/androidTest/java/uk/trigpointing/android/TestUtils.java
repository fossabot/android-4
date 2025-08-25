package uk.trigpointing.android;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import uk.trigpointing.android.types.Condition;
import uk.trigpointing.android.types.PhotoSubject;
import uk.trigpointing.android.types.Trig;

/**
 * Utility class for Android tests
 * Provides common setup, test data creation, and helper methods
 */
public class TestUtils {

    /**
     * Creates a test trigpoint with default values for testing
     */
    public static long createTestTrig(DbHelper dbHelper, long id, String name) {
        return dbHelper.createTrig(
            id, name, "TEST" + id, 54.5270, -3.0165,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, "Test FB content"
        );
    }

    /**
     * Creates a test trigpoint with custom coordinates
     */
    public static long createTestTrigAt(DbHelper dbHelper, long id, String name, double lat, double lon) {
        return dbHelper.createTrig(
            id, name, "TEST" + id, lat, lon,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null
        );
    }

    /**
     * Creates a test log entry
     */
    public static long createTestLog(DbHelper dbHelper, long trigId) {
        return dbHelper.createLog(
            trigId, 2024, 12, 25, 1640, 14, 30,
            "NY341151", "Test FB", Condition.GOOD, 10,
            "Test log comment", 0, 0
        );
    }

    /**
     * Creates a test photo
     */
    public static long createTestPhoto(DbHelper dbHelper, long trigId) {
        return dbHelper.createPhoto(
            trigId, "Test Photo", "Test Description",
            "test_icon.jpg", "test_photo.jpg", PhotoSubject.AERIAL, 1
        );
    }

    /**
     * Sets up test preferences for LeafletMapActivity tests
     */
    public static void setupTestPreferences(String apiKey, String mapStyle) {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putString("os_api_key", apiKey)
            .putString("leaflet_map_style", mapStyle)
            .apply();
    }

    /**
     * Clears all test preferences
     */
    public static void clearTestPreferences() {
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().apply();
    }

    /**
     * Sets up a clean database for testing
     */
    public static void setupCleanDatabase(DbHelper dbHelper) {
        dbHelper.deleteAll();
        dbHelper.clearUserLogs();
    }

    /**
     * Creates a set of test trigpoints for list/map testing
     */
    public static void createTestTrigpointSet(DbHelper dbHelper) {
        // Lake District trigpoints for testing
        createTestTrigAt(dbHelper, 1001, "Helvellyn", 54.5270, -3.0165);
        createTestTrigAt(dbHelper, 1002, "Scafell Pike", 54.4542, -3.2085);
        createTestTrigAt(dbHelper, 1003, "Skiddaw", 54.6511, -3.1470);
        createTestTrigAt(dbHelper, 1004, "Great Langdale", 54.4380, -3.0800);
        createTestTrigAt(dbHelper, 1005, "Coniston Old Man", 54.3706, -3.0985);
    }

    /**
     * Creates test data with different trig types for counting tests
     */
    public static void createTestTrigpointTypes(DbHelper dbHelper) {
        // Pillars
        dbHelper.createTrig(2001, "Pillar 1", "P001", 54.0, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
        
        dbHelper.createTrig(2002, "Pillar 2", "P002", 54.1, -3.1,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);

        // FBMs
        dbHelper.createTrig(2003, "FBM 1", "F001", 54.2, -3.2,
            Trig.Physical.FBM, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);

        // Intersected
        dbHelper.createTrig(2004, "Intersected 1", "I001", 54.3, -3.3,
            Trig.Physical.INTERSECTED, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);

        // Passive (other type)
        dbHelper.createTrig(2005, "Passive 1", "PA001", 54.4, -3.4,
            Trig.Physical.PASSIVE, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
    }

    /**
     * Waits for a specified duration (useful for UI tests)
     */
    public static void waitFor(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Common test constants
     */
    public static class Constants {
        public static final String TEST_API_KEY = "test_api_key_123";
        public static final String TEST_MAP_STYLE = "OpenStreetMap";
        public static final double HELVELLYN_LAT = 54.5270;
        public static final double HELVELLYN_LON = -3.0165;
        public static final String HELVELLYN_GRIDREF = "NY341151";
        
        // Timeout values for UI tests
        public static final int SHORT_TIMEOUT = 5000;   // 5 seconds
        public static final int MEDIUM_TIMEOUT = 10000; // 10 seconds
        public static final int LONG_TIMEOUT = 20000;   // 20 seconds
    }
}

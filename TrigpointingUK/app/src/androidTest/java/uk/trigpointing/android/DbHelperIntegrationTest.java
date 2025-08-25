package uk.trigpointing.android;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.trigpointing.android.types.Condition;
import uk.trigpointing.android.types.PhotoSubject;
import uk.trigpointing.android.types.Trig;
import uk.trigpointing.android.mapping.BoundingBox;

import static org.junit.Assert.*;

/**
 * Integration tests for DbHelper class
 * Tests database operations, CRUD functionality, and data integrity
 */
@RunWith(AndroidJUnit4.class)
public class DbHelperIntegrationTest {

    private DbHelper dbHelper;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = new DbHelper(context);
        dbHelper.open();
        
        // Clean up any existing data
        dbHelper.deleteAll();
        dbHelper.clearUserLogs();
    }

    @After
    public void tearDown() throws Exception {
        if (dbHelper != null) {
            // Clean up test data
            dbHelper.deleteAll();
            dbHelper.clearUserLogs();
            dbHelper.close();
        }
    }

    @Test
    public void testDatabaseInitialization() {
        // Test that database opens successfully
        assertNotNull("DbHelper should be initialized", dbHelper);
        assertNotNull("Database should be open", dbHelper.mDb);
        assertTrue("Database should be open", dbHelper.mDb.isOpen());
    }

    @Test
    public void testCreateAndRetrieveTrig() {
        // Create test trigpoint (Helvellyn)
        long trigId = 1001;
        String name = "Helvellyn";
        String waypoint = "LD0001";
        double lat = 54.5270;
        double lon = -3.0165;
        
        long result = dbHelper.createTrig(
            trigId, name, waypoint, lat, lon,
            Trig.Physical.PILLAR, 
            Condition.GOOD, 
            Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, 
            Trig.Historic.HISTORIC,
            "Test FB content"
        );
        
        assertTrue("Trig creation should succeed", result != -1);
        
        // Retrieve the trigpoint
        Cursor cursor = dbHelper.fetchTrigInfo(trigId);
        assertNotNull("Cursor should not be null", cursor);
        assertTrue("Cursor should have data", cursor.moveToFirst());
        
        assertEquals("Name should match", name, cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_NAME)));
        assertEquals("Waypoint should match", waypoint, cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_WAYPOINT)));
        assertEquals("Latitude should match", lat, cursor.getDouble(cursor.getColumnIndex(DbHelper.TRIG_LAT)), 0.0001);
        assertEquals("Longitude should match", lon, cursor.getDouble(cursor.getColumnIndex(DbHelper.TRIG_LON)), 0.0001);
        assertEquals("Type should match", Trig.Physical.PILLAR.code(), cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_TYPE)));
        
        cursor.close();
    }

    @Test
    public void testUpdateTrigLog() {
        // Create test trigpoint
        long trigId = 1002;
        dbHelper.createTrig(
            trigId, "Test Trig", "TEST001", 54.0, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null
        );
        
        // Update log status
        boolean updateResult = dbHelper.updateTrigLog(trigId, Condition.GOOD);
        assertTrue("Update should succeed", updateResult);
        
        // Verify update
        Cursor cursor = dbHelper.fetchTrigInfo(trigId);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("Log status should be updated", 
            Condition.GOOD.code(), 
            cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_LOGGED)));
        cursor.close();
    }

    @Test
    public void testTrigTablePopulation() {
        // Initially empty
        assertFalse("Table should be empty initially", dbHelper.isTrigTablePopulated());
        
        // Add a trigpoint
        dbHelper.createTrig(
            1003, "Test Trig", "TEST001", 54.0, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null
        );
        
        // Now should be populated
        assertTrue("Table should be populated after adding trig", dbHelper.isTrigTablePopulated());
    }

    @Test
    public void testCountingMethods() {
        // Add test trigpoints of different types
        dbHelper.createTrig(1011, "Pillar 1", "P001", 54.0, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
            
        dbHelper.createTrig(1012, "Pillar 2", "P002", 54.1, -3.1,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
            
        dbHelper.createTrig(1013, "FBM 1", "F001", 54.2, -3.2,
            Trig.Physical.FBM, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
            
        dbHelper.createTrig(1014, "Intersected 1", "I001", 54.3, -3.3,
            Trig.Physical.INTERSECTED, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
        
        // Test counting methods
        assertEquals("Should count 2 logged pillars", 2, dbHelper.countLoggedPillars());
        assertEquals("Should count 1 logged FBM", 1, dbHelper.countLoggedFbms());
        assertEquals("Should count 1 logged intersected", 1, dbHelper.countLoggedIntersecteds());
        assertEquals("Should count 0 logged passives", 0, dbHelper.countLoggedPassives());
    }

    @Test
    public void testLogOperations() {
        // Create a log entry
        long logId = 2001;
        long result = dbHelper.createLog(
            logId, 2024, 12, 25, 1640, 14, 30,
            "NY341151", "Test FB", Condition.GOOD, 10,
            "Test log comment", 0, 0
        );
        
        assertTrue("Log creation should succeed", result != -1);
        assertEquals("Should count 1 unsynced log", 1, dbHelper.countUnsynced());
        
        // Fetch the log
        Cursor cursor = dbHelper.fetchLog(logId);
        assertNotNull("Log cursor should not be null", cursor);
        assertTrue("Log cursor should have data", cursor.moveToFirst());
        
        assertEquals("Year should match", 2024, cursor.getInt(cursor.getColumnIndex(DbHelper.LOG_YEAR)));
        assertEquals("Month should match", 12, cursor.getInt(cursor.getColumnIndex(DbHelper.LOG_MONTH)));
        assertEquals("Day should match", 25, cursor.getInt(cursor.getColumnIndex(DbHelper.LOG_DAY)));
        assertEquals("Grid ref should match", "NY341151", cursor.getString(cursor.getColumnIndex(DbHelper.LOG_GRIDREF)));
        
        cursor.close();
        
        // Delete the log
        boolean deleteResult = dbHelper.deleteLog(logId);
        assertTrue("Log deletion should succeed", deleteResult);
        assertEquals("Should count 0 unsynced logs after deletion", 0, dbHelper.countUnsynced());
    }

    @Test
    public void testPhotoOperations() {
        // Create a trigpoint first
        long trigId = 3001;
        dbHelper.createTrig(
            trigId, "Photo Test Trig", "PT001", 54.0, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null
        );
        
        // Create a photo
        long photoResult = dbHelper.createPhoto(
            trigId, "Test Photo", "Test Description",
            "icon.jpg", "photo.jpg", PhotoSubject.AERIAL, 1
        );
        
        assertTrue("Photo creation should succeed", photoResult != -1);
        assertEquals("Should count 1 photo", 1, dbHelper.countPhotos());
        
        // Fetch the photo
        Cursor cursor = dbHelper.fetchPhoto(photoResult);
        assertNotNull("Photo cursor should not be null", cursor);
        assertTrue("Photo cursor should have data", cursor.moveToFirst());
        
        assertEquals("Trig ID should match", trigId, cursor.getLong(cursor.getColumnIndex(DbHelper.PHOTO_TRIG)));
        assertEquals("Name should match", "Test Photo", cursor.getString(cursor.getColumnIndex(DbHelper.PHOTO_NAME)));
        assertEquals("Description should match", "Test Description", cursor.getString(cursor.getColumnIndex(DbHelper.PHOTO_DESCR)));
        
        cursor.close();
        
        // Test photo deletion
        boolean deleteResult = dbHelper.deletePhoto(photoResult);
        assertTrue("Photo deletion should succeed", deleteResult);
        assertEquals("Should count 0 photos after deletion", 0, dbHelper.countPhotos());
    }

    @Test
    public void testMarkedTrigOperations() {
        long trigId = 4001;
        
        // Initially not marked
        assertFalse("Trig should not be marked initially", dbHelper.isMarkedTrig(trigId));
        
        // Mark the trig
        Boolean markResult = dbHelper.setMarkedTrig(trigId, true);
        assertTrue("Marking should succeed", markResult);
        assertTrue("Trig should be marked after setting", dbHelper.isMarkedTrig(trigId));
        
        // Unmark the trig
        Boolean unmarkResult = dbHelper.setMarkedTrig(trigId, false);
        assertFalse("Unmarking should return false", unmarkResult);
        assertFalse("Trig should not be marked after unsetting", dbHelper.isMarkedTrig(trigId));
    }

    @Test
    public void testFetchTrigListWithLocation() {
        // Create test trigpoints at different distances
        dbHelper.createTrig(5001, "Near Trig", "N001", 54.5270, -3.0165,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
            
        dbHelper.createTrig(5002, "Far Trig", "F001", 55.0, -4.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
        
        // Create a location near the first trigpoint
        Location testLocation = new Location("test");
        testLocation.setLatitude(54.5270);
        testLocation.setLongitude(-3.0165);
        
        Cursor cursor = dbHelper.fetchTrigList(testLocation);
        assertNotNull("Cursor should not be null", cursor);
        assertTrue("Should have results", cursor.getCount() > 0);
        
        // First result should be the nearest trigpoint
        cursor.moveToFirst();
        assertEquals("First result should be nearest trig", "Near Trig", 
            cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_NAME)));
        
        cursor.close();
    }

    @Test
    public void testFetchTrigMapListWithBoundingBox() {
        // Create trigpoints inside and outside bounding box
        dbHelper.createTrig(6001, "Inside Trig", "I001", 54.5, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
            
        dbHelper.createTrig(6002, "Outside Trig", "O001", 55.5, -4.5,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
        
        // Create bounding box around Lake District
        BoundingBox boundingBox = new BoundingBox();
        boundingBox.setLatNorth(54.8);
        boundingBox.setLatSouth(54.2);
        boundingBox.setLonEast(-2.5);
        boundingBox.setLonWest(-3.5);
        
        Cursor cursor = dbHelper.fetchTrigMapList(boundingBox);
        assertNotNull("Cursor should not be null", cursor);
        assertEquals("Should have 1 result in bounding box", 1, cursor.getCount());
        
        cursor.moveToFirst();
        assertEquals("Result should be inside trig", "Inside Trig", 
            cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_NAME)));
        
        cursor.close();
    }

    @Test
    public void testClearUserLogs() {
        // Create test data
        long trigId = 7001;
        dbHelper.createTrig(trigId, "Test Trig", "T001", 54.0, -3.0,
            Trig.Physical.PILLAR, Condition.GOOD, Condition.GOOD,
            Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
            
        dbHelper.createLog(trigId, 2024, 12, 25, 1640, 14, 30,
            "NY341151", "Test", Condition.GOOD, 10, "Comment", 0, 0);
            
        dbHelper.createPhoto(trigId, "Photo", "Desc", "icon.jpg", "photo.jpg", 
            PhotoSubject.AERIAL, 1);
            
        dbHelper.setMarkedTrig(trigId, true);
        
        // Verify data exists
        assertTrue("Should have unsynced logs", dbHelper.countUnsynced() > 0);
        assertTrue("Should have photos", dbHelper.countPhotos() > 0);
        assertTrue("Should have logged pillars", dbHelper.countLoggedPillars() > 0);
        assertTrue("Trig should be marked", dbHelper.isMarkedTrig(trigId));
        
        // Clear user logs
        dbHelper.clearUserLogs();
        
        // Verify data is cleared
        assertEquals("Should have no unsynced logs", 0, dbHelper.countUnsynced());
        assertEquals("Should have no photos", 0, dbHelper.countPhotos());
        assertEquals("Should have no logged pillars", 0, dbHelper.countLoggedPillars());
        assertFalse("Trig should not be marked", dbHelper.isMarkedTrig(trigId));
        
        // But trigpoint should still exist
        assertTrue("Trig table should still be populated", dbHelper.isTrigTablePopulated());
    }

    @Test
    public void testDatabaseTransaction() {
        // Test that database operations work correctly with transactions
        long trigId = 8001;
        
        // This should work as one unit
        dbHelper.mDb.beginTransaction();
        try {
            dbHelper.createTrig(trigId, "Transaction Test", "T001", 54.0, -3.0,
                Trig.Physical.PILLAR, Condition.GOOD, Condition.TRIGNOTLOGGED,
                Trig.Current.CURRENT, Trig.Historic.HISTORIC, null);
                
            dbHelper.updateTrigLog(trigId, Condition.GOOD);
            
            dbHelper.mDb.setTransactionSuccessful();
        } finally {
            dbHelper.mDb.endTransaction();
        }
        
        // Verify the transaction completed successfully
        Cursor cursor = dbHelper.fetchTrigInfo(trigId);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("Log status should be updated", Condition.GOOD.code(), 
            cursor.getString(cursor.getColumnIndex(DbHelper.TRIG_LOGGED)));
        cursor.close();
    }
}

package uk.trigpointing.android.mapping;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for MapDownload class
 * Testing map download data structure and nested classes
 */
public class MapDownloadTest {

    private MapDownload testMapDownload;

    @Before
    public void setUp() {
        testMapDownload = new MapDownload();
    }

    @Test
    public void testMapDownloadCreation() {
        assertNotNull("MapDownload should be created", testMapDownload);
        assertNull("Initial name should be null", testMapDownload.name);
        assertNull("Initial description should be null", testMapDownload.description);
        assertNull("Initial source URL should be null", testMapDownload.sourceUrl);
        assertNull("Initial attribution should be null", testMapDownload.attribution);
        assertEquals("Initial min zoom should be 0", 0, testMapDownload.minZoom);
        assertEquals("Initial max zoom should be 0", 0, testMapDownload.maxZoom);
        assertNull("Initial bounds should be null", testMapDownload.bounds);
        assertNull("Initial type should be null", testMapDownload.type);
        assertNull("Initial format should be null", testMapDownload.format);
        assertNull("Initial file URL should be null", testMapDownload.fileUrl);
        assertEquals("Initial file size should be 0", 0L, testMapDownload.fileSize);
        assertNull("Initial file timestamp should be null", testMapDownload.fileTimestamp);
    }

    @Test
    public void testMapDownloadProperties() {
        // Test setting basic string properties
        testMapDownload.name = "Test Map";
        testMapDownload.description = "A test map for unit testing";
        testMapDownload.sourceUrl = "https://example.com/tiles/{z}/{x}/{y}.png";
        testMapDownload.attribution = "© Test Map Provider";
        testMapDownload.type = "raster";
        testMapDownload.format = "png";
        testMapDownload.fileUrl = "https://example.com/testmap.zip";
        testMapDownload.fileTimestamp = "2023-08-15T10:30:00Z";

        assertEquals("Name should be set", "Test Map", testMapDownload.name);
        assertEquals("Description should be set", "A test map for unit testing", testMapDownload.description);
        assertEquals("Source URL should be set", "https://example.com/tiles/{z}/{x}/{y}.png", testMapDownload.sourceUrl);
        assertEquals("Attribution should be set", "© Test Map Provider", testMapDownload.attribution);
        assertEquals("Type should be set", "raster", testMapDownload.type);
        assertEquals("Format should be set", "png", testMapDownload.format);
        assertEquals("File URL should be set", "https://example.com/testmap.zip", testMapDownload.fileUrl);
        assertEquals("File timestamp should be set", "2023-08-15T10:30:00Z", testMapDownload.fileTimestamp);
    }

    @Test
    public void testMapDownloadZoomLevels() {
        // Test zoom level properties
        testMapDownload.minZoom = 1;
        testMapDownload.maxZoom = 18;

        assertEquals("Min zoom should be set", 1, testMapDownload.minZoom);
        assertEquals("Max zoom should be set", 18, testMapDownload.maxZoom);

        // Test edge cases
        testMapDownload.minZoom = 0;
        testMapDownload.maxZoom = 25;

        assertEquals("Min zoom should handle 0", 0, testMapDownload.minZoom);
        assertEquals("Max zoom should handle 25", 25, testMapDownload.maxZoom);
    }

    @Test
    public void testMapDownloadBounds() {
        // Test bounds property with typical geographic bounds
        List<Double> testBounds = Arrays.asList(-180.0, -85.0, 180.0, 85.0); // [west, south, east, north]
        testMapDownload.bounds = testBounds;

        assertNotNull("Bounds should be set", testMapDownload.bounds);
        assertEquals("Bounds should have 4 elements", 4, testMapDownload.bounds.size());
        assertEquals("West bound should be -180", Double.valueOf(-180.0), testMapDownload.bounds.get(0));
        assertEquals("South bound should be -85", Double.valueOf(-85.0), testMapDownload.bounds.get(1));
        assertEquals("East bound should be 180", Double.valueOf(180.0), testMapDownload.bounds.get(2));
        assertEquals("North bound should be 85", Double.valueOf(85.0), testMapDownload.bounds.get(3));
    }

    @Test
    public void testMapDownloadFileSize() {
        // Test file size property
        testMapDownload.fileSize = 1024L;
        assertEquals("Small file size should be set", 1024L, testMapDownload.fileSize);

        testMapDownload.fileSize = 1048576L; // 1MB
        assertEquals("Medium file size should be set", 1048576L, testMapDownload.fileSize);

        testMapDownload.fileSize = 1073741824L; // 1GB
        assertEquals("Large file size should be set", 1073741824L, testMapDownload.fileSize);

        testMapDownload.fileSize = 0L;
        assertEquals("Zero file size should be set", 0L, testMapDownload.fileSize);
    }

    @Test
    public void testMapDownloadWithCompleteData() {
        // Test a complete map download object
        testMapDownload.name = "OpenStreetMap";
        testMapDownload.description = "Standard OpenStreetMap tiles";
        testMapDownload.sourceUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
        testMapDownload.attribution = "© OpenStreetMap contributors";
        testMapDownload.minZoom = 1;
        testMapDownload.maxZoom = 19;
        testMapDownload.bounds = Arrays.asList(-180.0, -85.0511, 180.0, 85.0511);
        testMapDownload.type = "raster";
        testMapDownload.format = "png";
        testMapDownload.fileUrl = "https://example.com/osm_tiles.tar.gz";
        testMapDownload.fileSize = 2147483648L; // 2GB
        testMapDownload.fileTimestamp = "2023-08-15T12:00:00Z";

        // Verify all properties
        assertEquals("Complete name", "OpenStreetMap", testMapDownload.name);
        assertEquals("Complete description", "Standard OpenStreetMap tiles", testMapDownload.description);
        assertEquals("Complete source URL", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", testMapDownload.sourceUrl);
        assertEquals("Complete attribution", "© OpenStreetMap contributors", testMapDownload.attribution);
        assertEquals("Complete min zoom", 1, testMapDownload.minZoom);
        assertEquals("Complete max zoom", 19, testMapDownload.maxZoom);
        assertNotNull("Complete bounds", testMapDownload.bounds);
        assertEquals("Complete type", "raster", testMapDownload.type);
        assertEquals("Complete format", "png", testMapDownload.format);
        assertEquals("Complete file URL", "https://example.com/osm_tiles.tar.gz", testMapDownload.fileUrl);
        assertEquals("Complete file size", 2147483648L, testMapDownload.fileSize);
        assertEquals("Complete file timestamp", "2023-08-15T12:00:00Z", testMapDownload.fileTimestamp);
    }

    @Test
    public void testMapDownloadsListCreation() {
        MapDownload.MapDownloadsList mapList = new MapDownload.MapDownloadsList();
        assertNotNull("MapDownloadsList should be created", mapList);
        assertNull("Initial creator should be null", mapList.creator);
        assertNull("Initial version should be null", mapList.version);
        assertNull("Initial timestamp should be null", mapList.timestamp);
        assertNull("Initial email should be null", mapList.email);
        assertNull("Initial maps should be null", mapList.maps);
    }

    @Test
    public void testMapDownloadsListProperties() {
        MapDownload.MapDownloadsList mapList = new MapDownload.MapDownloadsList();
        
        mapList.creator = "TrigpointingUK";
        mapList.version = "1.0";
        mapList.timestamp = "2023-08-15T10:00:00Z";
        mapList.email = "admin@trigpointing.uk";
        mapList.maps = Arrays.asList(testMapDownload);

        assertEquals("Creator should be set", "TrigpointingUK", mapList.creator);
        assertEquals("Version should be set", "1.0", mapList.version);
        assertEquals("Timestamp should be set", "2023-08-15T10:00:00Z", mapList.timestamp);
        assertEquals("Email should be set", "admin@trigpointing.uk", mapList.email);
        assertNotNull("Maps should be set", mapList.maps);
        assertEquals("Maps should contain one item", 1, mapList.maps.size());
        assertEquals("First map should be test map", testMapDownload, mapList.maps.get(0));
    }

    @Test
    public void testMapDownloadsListWithMultipleMaps() {
        MapDownload.MapDownloadsList mapList = new MapDownload.MapDownloadsList();
        
        // Create multiple map downloads
        MapDownload map1 = new MapDownload();
        map1.name = "Map 1";
        map1.type = "raster";
        
        MapDownload map2 = new MapDownload();
        map2.name = "Map 2";
        map2.type = "vector";
        
        mapList.maps = Arrays.asList(map1, map2);

        assertNotNull("Maps list should be set", mapList.maps);
        assertEquals("Should have 2 maps", 2, mapList.maps.size());
        assertEquals("First map name", "Map 1", mapList.maps.get(0).name);
        assertEquals("First map type", "raster", mapList.maps.get(0).type);
        assertEquals("Second map name", "Map 2", mapList.maps.get(1).name);
        assertEquals("Second map type", "vector", mapList.maps.get(1).type);
    }

    @Test
    public void testMapDownloadPublicFields() {
        // Test that all fields are public (as expected for data classes)
        try {
            // Verify fields are public by accessing them
            testMapDownload.name = "Public Test";
            testMapDownload.description = "Testing public access";
            testMapDownload.minZoom = 5;
            testMapDownload.maxZoom = 15;
            testMapDownload.fileSize = 1000L;
            
            assertEquals("Public name access", "Public Test", testMapDownload.name);
            assertEquals("Public description access", "Testing public access", testMapDownload.description);
            assertEquals("Public min zoom access", 5, testMapDownload.minZoom);
            assertEquals("Public max zoom access", 15, testMapDownload.maxZoom);
            assertEquals("Public file size access", 1000L, testMapDownload.fileSize);
            
            // This test confirms the fields are public and accessible
            assertTrue("All fields should be publicly accessible", true);
        } catch (Exception e) {
            fail("Fields should be public and accessible: " + e.getMessage());
        }
    }

    @Test
    public void testMapDownloadEdgeCases() {
        // Test edge cases and boundary values
        testMapDownload.name = "";
        testMapDownload.description = "";
        testMapDownload.minZoom = -1;
        testMapDownload.maxZoom = Integer.MAX_VALUE;
        testMapDownload.fileSize = Long.MAX_VALUE;
        testMapDownload.bounds = Arrays.asList();

        assertEquals("Empty name", "", testMapDownload.name);
        assertEquals("Empty description", "", testMapDownload.description);
        assertEquals("Negative min zoom", -1, testMapDownload.minZoom);
        assertEquals("Max integer max zoom", Integer.MAX_VALUE, testMapDownload.maxZoom);
        assertEquals("Max long file size", Long.MAX_VALUE, testMapDownload.fileSize);
        assertNotNull("Empty bounds list", testMapDownload.bounds);
        assertEquals("Empty bounds size", 0, testMapDownload.bounds.size());
    }
}

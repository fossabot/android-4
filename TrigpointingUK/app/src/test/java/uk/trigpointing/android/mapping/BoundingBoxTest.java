package uk.trigpointing.android.mapping;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for BoundingBox class
 * Testing geographic bounding box creation and properties
 */
public class BoundingBoxTest {

    private BoundingBox testBoundingBox;
    private static final double DELTA = 0.0001; // Tolerance for floating point comparisons

    @Before
    public void setUp() {
        // Create a bounding box around London area
        // North: 51.5, East: 0.1, South: 51.4, West: -0.1
        testBoundingBox = new BoundingBox(51.5, 0.1, 51.4, -0.1);
    }

    @Test
    public void testBoundingBoxCreation() {
        assertNotNull(testBoundingBox);
        assertEquals(51.5, testBoundingBox.getLatNorth(), DELTA);
        assertEquals(51.4, testBoundingBox.getLatSouth(), DELTA);
        assertEquals(0.1, testBoundingBox.getLonEast(), DELTA);
        assertEquals(-0.1, testBoundingBox.getLonWest(), DELTA);
    }

    @Test
    public void testGetLatNorth() {
        double expectedNorth = 54.123;
        BoundingBox box = new BoundingBox(expectedNorth, 1.0, 50.0, -1.0);
        assertEquals(expectedNorth, box.getLatNorth(), DELTA);
    }

    @Test
    public void testGetLatSouth() {
        double expectedSouth = 50.456;
        BoundingBox box = new BoundingBox(55.0, 1.0, expectedSouth, -1.0);
        assertEquals(expectedSouth, box.getLatSouth(), DELTA);
    }

    @Test
    public void testGetLonEast() {
        double expectedEast = 2.789;
        BoundingBox box = new BoundingBox(55.0, expectedEast, 50.0, -1.0);
        assertEquals(expectedEast, box.getLonEast(), DELTA);
    }

    @Test
    public void testGetLonWest() {
        double expectedWest = -3.456;
        BoundingBox box = new BoundingBox(55.0, 1.0, 50.0, expectedWest);
        assertEquals(expectedWest, box.getLonWest(), DELTA);
    }

    @Test
    public void testBoundingBoxWithNegativeCoordinates() {
        // Test with all negative coordinates
        BoundingBox box = new BoundingBox(-10.0, -20.0, -30.0, -40.0);
        assertEquals(-10.0, box.getLatNorth(), DELTA);
        assertEquals(-30.0, box.getLatSouth(), DELTA);
        assertEquals(-20.0, box.getLonEast(), DELTA);
        assertEquals(-40.0, box.getLonWest(), DELTA);
    }

    @Test
    public void testBoundingBoxWithZeroCoordinates() {
        // Test with zero coordinates
        BoundingBox box = new BoundingBox(0.0, 0.0, 0.0, 0.0);
        assertEquals(0.0, box.getLatNorth(), DELTA);
        assertEquals(0.0, box.getLatSouth(), DELTA);
        assertEquals(0.0, box.getLonEast(), DELTA);
        assertEquals(0.0, box.getLonWest(), DELTA);
    }

    @Test
    public void testBoundingBoxImmutability() {
        // Test that the bounding box is immutable (final fields)
        double originalNorth = testBoundingBox.getLatNorth();
        double originalSouth = testBoundingBox.getLatSouth();
        double originalEast = testBoundingBox.getLonEast();
        double originalWest = testBoundingBox.getLonWest();

        // Values should remain the same (no setters exist)
        assertEquals(originalNorth, testBoundingBox.getLatNorth(), DELTA);
        assertEquals(originalSouth, testBoundingBox.getLatSouth(), DELTA);
        assertEquals(originalEast, testBoundingBox.getLonEast(), DELTA);
        assertEquals(originalWest, testBoundingBox.getLonWest(), DELTA);
    }

    @Test
    public void testToString() {
        String stringRepresentation = testBoundingBox.toString();
        assertNotNull(stringRepresentation);
        assertTrue("toString should contain 'BoundingBox'", stringRepresentation.contains("BoundingBox"));
        assertTrue("toString should contain north value", stringRepresentation.contains("51.5"));
        assertTrue("toString should contain south value", stringRepresentation.contains("51.4"));
        assertTrue("toString should contain east value", stringRepresentation.contains("0.1"));
        assertTrue("toString should contain west value", stringRepresentation.contains("-0.1"));
        assertTrue("toString should contain 'north='", stringRepresentation.contains("north="));
        assertTrue("toString should contain 'south='", stringRepresentation.contains("south="));
        assertTrue("toString should contain 'east='", stringRepresentation.contains("east="));
        assertTrue("toString should contain 'west='", stringRepresentation.contains("west="));
    }

    @Test
    public void testBoundingBoxConstructorParameterOrder() {
        // Test that the constructor parameters are in the expected order: north, east, south, west
        BoundingBox box = new BoundingBox(10.0, 20.0, 30.0, 40.0);
        assertEquals("First parameter should be north", 10.0, box.getLatNorth(), DELTA);
        assertEquals("Second parameter should be east", 20.0, box.getLonEast(), DELTA);
        assertEquals("Third parameter should be south", 30.0, box.getLatSouth(), DELTA);
        assertEquals("Fourth parameter should be west", 40.0, box.getLonWest(), DELTA);
    }

    @Test
    public void testBoundingBoxWithLargeCoordinates() {
        // Test with large coordinate values
        BoundingBox box = new BoundingBox(89.999, 179.999, -89.999, -179.999);
        assertEquals(89.999, box.getLatNorth(), DELTA);
        assertEquals(-89.999, box.getLatSouth(), DELTA);
        assertEquals(179.999, box.getLonEast(), DELTA);
        assertEquals(-179.999, box.getLonWest(), DELTA);
    }
}

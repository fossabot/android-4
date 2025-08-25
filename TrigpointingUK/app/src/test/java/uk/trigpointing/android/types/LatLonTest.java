package uk.trigpointing.android.types;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import uk.trigpointing.android.types.LatLon.UNITS;

/**
 * Unit tests for LatLon class
 * Testing coordinate conversions, distance calculations, and grid reference parsing
 */
public class LatLonTest {

    private LatLon testLocation;
    private static final double DELTA = 0.001; // Tolerance for floating point comparisons

    @Before
    public void setUp() {
        // Use a known trigpoint location: Helvellyn (Lake District)
        testLocation = new LatLon(54.5270, -3.0165);
    }

    @Test
    public void testConstructorWithLatLon() {
        LatLon location = new LatLon(54.5270, -3.0165);
        assertEquals(54.5270, location.getLat(), DELTA);
        assertEquals(-3.0165, location.getLon(), DELTA);
    }

    @Test
    public void testOSGBGridReferenceConversion() {
        // Test known conversions for Helvellyn
        // NY341151 should be approximately 54.5270, -3.0165
        LatLon fromGridRef = new LatLon("NY341151");
        
        assertNotNull(fromGridRef.getLat());
        assertNotNull(fromGridRef.getLon());
        
        // Should be within reasonable tolerance (grid refs are approximate)
        assertEquals(54.527, fromGridRef.getLat(), 0.01);
        assertEquals(-3.016, fromGridRef.getLon(), 0.01);
    }

    @Test
    public void testOSGBGridReferenceGeneration() {
        LatLon location = new LatLon(54.5270, -3.0165);
        String gridRef6 = location.getOSGB6();
        String gridRef10 = location.getOSGB10();
        
        assertNotNull(gridRef6);
        assertNotNull(gridRef10);
        assertTrue("Grid ref should start with NY", gridRef6.startsWith("NY"));
        assertTrue("Grid ref should start with NY", gridRef10.startsWith("NY"));
        assertEquals(8, gridRef6.length()); // NY341151 format
        assertEquals(13, gridRef10.length()); // NY 34100 15100 format
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidGridReferenceThrowsException() {
        new LatLon("INVALID");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyGridReferenceThrowsException() {
        new LatLon("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullGridReferenceThrowsException() {
        new LatLon((String) null);
    }

    @Test
    public void testDistanceCalculationInKilometers() {
        // Distance from Helvellyn to Scafell Pike (approximately 20km)
        LatLon scafellPike = new LatLon(54.4542, -3.2085);
        Double distance = testLocation.distanceTo(scafellPike, UNITS.KM);
        
        assertNotNull(distance);
        // Should be approximately 20km (allowing reasonable tolerance)
        assertTrue("Distance should be between 15-25km", distance > 15 && distance < 25);
    }

    @Test
    public void testDistanceCalculationInMiles() {
        LatLon scafellPike = new LatLon(54.4542, -3.2085);
        Double distanceKm = testLocation.distanceTo(scafellPike, UNITS.KM);
        Double distanceMiles = testLocation.distanceTo(scafellPike, UNITS.MILES);
        
        assertNotNull(distanceKm);
        assertNotNull(distanceMiles);
        
        // Miles should be approximately 0.621 times kilometers
        assertEquals(distanceKm * 0.621, distanceMiles, 1.0);
    }

    @Test
    public void testDistanceCalculationInMeters() {
        LatLon scafellPike = new LatLon(54.4542, -3.2085);
        Double distanceKm = testLocation.distanceTo(scafellPike, UNITS.KM);
        Double distanceM = testLocation.distanceTo(scafellPike, UNITS.METRES);
        
        assertNotNull(distanceKm);
        assertNotNull(distanceM);
        
        // Meters should be 1000 times kilometers
        assertEquals(distanceKm * 1000, distanceM, 100.0);
    }

    @Test
    public void testDistanceToNullCoordinatesReturnsNull() {
        Double distance = testLocation.distanceTo(null, null, UNITS.KM);
        assertNull(distance);
    }

    @Test
    public void testBearingCalculation() {
        // Bearing from Helvellyn to Scafell Pike (roughly southwest)
        LatLon scafellPike = new LatLon(54.4542, -3.2085);
        Double bearing = testLocation.bearingTo(scafellPike);
        
        assertNotNull(bearing);
        // Should be southwest quadrant (roughly 180-270 degrees)
        assertTrue("Bearing should be southwest", bearing > 180 && bearing < 270);
    }

    @Test
    public void testBearingFromCalculation() {
        LatLon scafellPike = new LatLon(54.4542, -3.2085);
        Double bearingFrom = testLocation.bearingFrom(scafellPike);
        
        assertNotNull(bearingFrom);
        // Should be between 0-360 degrees
        assertTrue("Bearing should be normalized", bearingFrom >= 0 && bearingFrom <= 360);
    }

    @Test
    public void testBearingFromNullCoordinatesReturnsNull() {
        Double bearing = testLocation.bearingFrom(null, null);
        assertNull(bearing);
    }

    @Test
    public void testWGSFormatting() {
        LatLon location = new LatLon(54.5270, -3.0165);
        String wgs = location.getWGS();
        
        assertNotNull(wgs);
        assertTrue("Should contain latitude", wgs.contains("N54"));
        assertTrue("Should contain longitude", wgs.contains("W003"));
    }

    @Test
    public void testToStringFormatting() {
        LatLon location = new LatLon(54.5270, -3.0165);
        String str = location.toString();
        
        assertNotNull(str);
        assertTrue("Should contain coordinates", str.contains("54") && str.contains("-3"));
    }

    @Test
    public void testEastingsNorthingsConversion() {
        LatLon location = new LatLon(54.5270, -3.0165);
        
        Long eastings = location.getEastings();
        Long northings = location.getNorthings();
        
        assertNotNull(eastings);
        assertNotNull(northings);
        
        // Should be reasonable OSGB coordinates for Lake District
        assertTrue("Eastings should be reasonable", eastings > 300000 && eastings < 400000);
        assertTrue("Northings should be reasonable", northings > 500000 && northings < 600000);
    }

    @Test
    public void testRoundTripConversion() {
        // Test that converting lat/lon -> grid ref -> lat/lon preserves accuracy
        LatLon original = new LatLon(54.5270, -3.0165);
        String gridRef = original.getOSGB10();
        LatLon converted = new LatLon(gridRef);
        
        // Should be within 10 meters (reasonable tolerance for grid ref precision)
        Double distance = original.distanceTo(converted, UNITS.METRES);
        assertNotNull(distance);
        assertTrue("Round trip conversion should be accurate", distance < 10.0);
    }

    @Test
    public void testSettersInvalidateCache() {
        LatLon location = new LatLon(54.5270, -3.0165);
        Long originalEastings = location.getEastings();
        
        // Change coordinates
        location.setLat(55.0);
        location.setLon(-3.5);
        
        // Eastings should be different after coordinate change
        Long newEastings = location.getEastings();
        assertNotEquals("Eastings should change when coordinates change", originalEastings, newEastings);
    }

    @Test
    public void testGridReferenceWithDifferentPrecisions() {
        // Test 6, 8, and 10 digit grid references
        assertGridRefParsing("NY341151", 6);  // 6 digits
        assertGridRefParsing("NY34111510", 8); // 8 digits  
        assertGridRefParsing("NY3410015100", 10); // 10 digits
    }

    private void assertGridRefParsing(String gridRef, int expectedDigits) {
        try {
            LatLon location = new LatLon(gridRef);
            assertNotNull("Should parse " + expectedDigits + " digit grid ref", location.getLat());
            assertNotNull("Should parse " + expectedDigits + " digit grid ref", location.getLon());
        } catch (IllegalArgumentException e) {
            fail("Should successfully parse " + expectedDigits + " digit grid ref: " + gridRef);
        }
    }

    @Test
    public void testGridReferenceIgnoresWhitespace() {
        LatLon location1 = new LatLon("NY341151");
        LatLon location2 = new LatLon("NY 341 151");
        LatLon location3 = new LatLon(" NY341151 ");
        
        assertEquals("Whitespace should be ignored", location1.getLat(), location2.getLat(), DELTA);
        assertEquals("Whitespace should be ignored", location1.getLat(), location3.getLat(), DELTA);
        assertEquals("Whitespace should be ignored", location1.getLon(), location2.getLon(), DELTA);
        assertEquals("Whitespace should be ignored", location1.getLon(), location3.getLon(), DELTA);
    }
}

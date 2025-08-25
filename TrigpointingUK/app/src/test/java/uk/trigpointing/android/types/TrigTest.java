package uk.trigpointing.android.types;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import uk.trigpointing.android.R;

/**
 * Unit tests for Trig class
 * Testing trigpoint data structure, enums, and business logic
 */
public class TrigTest {

    private Trig testTrig;

    @Before
    public void setUp() {
        testTrig = new Trig();
    }

    @Test
    public void testTrigCreation() {
        assertNotNull(testTrig);
        assertNull(testTrig.getName());
        assertNull(testTrig.getDistance());
        assertNull(testTrig.getBearing());
        assertEquals(0, testTrig.getType());
        assertNull(testTrig.getCondition());
        assertEquals(0, testTrig.getFound());
    }

    @Test
    public void testNameProperty() {
        String testName = "Test Trig Point";
        testTrig.setName(testName);
        assertEquals(testName, testTrig.getName());
    }

    @Test
    public void testDistanceProperty() {
        String testDistance = "1.5km";
        testTrig.setDistance(testDistance);
        assertEquals(testDistance, testTrig.getDistance());
    }

    @Test
    public void testBearingProperty() {
        Double testBearing = 45.5;
        testTrig.setBearing(testBearing);
        assertEquals(testBearing, testTrig.getBearing());
    }

    @Test
    public void testTypeProperty() {
        int testType = 5;
        testTrig.setType(testType);
        assertEquals(testType, testTrig.getType());
    }

    @Test
    public void testConditionProperty() {
        Condition testCondition = Condition.GOOD;
        testTrig.setCondition(testCondition);
        assertEquals(testCondition, testTrig.getCondition());
    }

    @Test
    public void testFoundProperty() {
        int foundStatus = 1;
        testTrig.setFound(foundStatus);
        assertEquals(foundStatus, testTrig.getFound());
    }

    @Test
    public void testPhysicalEnumValues() {
        // Test enum constants exist and have expected properties
        assertNotNull(Trig.Physical.PILLAR);
        assertEquals("PI", Trig.Physical.PILLAR.code());
        assertEquals("Pillar", Trig.Physical.PILLAR.toString());
        assertEquals(R.drawable.t_pillar, Trig.Physical.PILLAR.icon());
        assertEquals(R.drawable.ts_pillar, Trig.Physical.PILLAR.icon(true));
        
        assertNotNull(Trig.Physical.FBM);
        assertEquals("FB", Trig.Physical.FBM.code());
        assertEquals("FBM", Trig.Physical.FBM.toString());
        assertEquals(R.drawable.t_fbm, Trig.Physical.FBM.icon());
        
        assertNotNull(Trig.Physical.INTERSECTED);
        assertEquals("IN", Trig.Physical.INTERSECTED.code());
        assertEquals("Intersected Station", Trig.Physical.INTERSECTED.toString());
    }

    @Test
    public void testPhysicalEnumFromCode() {
        assertEquals(Trig.Physical.PILLAR, Trig.Physical.fromCode("PI"));
        assertEquals(Trig.Physical.FBM, Trig.Physical.fromCode("FB"));
        assertEquals(Trig.Physical.INTERSECTED, Trig.Physical.fromCode("IN"));
        assertEquals(Trig.Physical.ACTIVE, Trig.Physical.fromCode("AC"));
        
        // Test unknown code
        assertEquals(Trig.Physical.OTHER, Trig.Physical.fromCode("UNKNOWN"));
        assertEquals(Trig.Physical.OTHER, Trig.Physical.fromCode(null));
    }

    @Test
    public void testCurrentEnumValues() {
        // Test Current enum
        assertNotNull(Trig.Current.ACTIVE);
        assertEquals("A", Trig.Current.ACTIVE.code());
        assertEquals("Active", Trig.Current.ACTIVE.toString());
        
        assertNotNull(Trig.Current.PASSIVE);
        assertEquals("P", Trig.Current.PASSIVE.code());
        assertEquals("Passive", Trig.Current.PASSIVE.toString());
        
        assertNotNull(Trig.Current.GPS);
        assertEquals("G", Trig.Current.GPS.code());
        assertEquals("GPS Station", Trig.Current.GPS.toString());
    }

    @Test
    public void testCurrentEnumFromCode() {
        assertEquals(Trig.Current.ACTIVE, Trig.Current.fromCode("A"));
        assertEquals(Trig.Current.PASSIVE, Trig.Current.fromCode("P"));
        assertEquals(Trig.Current.GPS, Trig.Current.fromCode("G"));
        assertEquals(Trig.Current.NCE, Trig.Current.fromCode("C"));
        
        // Test unknown code
        assertEquals(Trig.Current.NONE, Trig.Current.fromCode("UNKNOWN"));
        assertEquals(Trig.Current.NONE, Trig.Current.fromCode(null));
    }

    @Test
    public void testHistoricEnumValues() {
        assertNotNull(Trig.Historic.PRIMARY);
        assertEquals("1", Trig.Historic.PRIMARY.code());
        assertEquals("Primary", Trig.Historic.PRIMARY.toString());
        
        assertNotNull(Trig.Historic.SECONDARY);
        assertEquals("2", Trig.Historic.SECONDARY.code());
        assertEquals("Secondary", Trig.Historic.SECONDARY.toString());
    }

    @Test
    public void testHistoricEnumFromCode() {
        assertEquals(Trig.Historic.PRIMARY, Trig.Historic.fromCode("1"));
        assertEquals(Trig.Historic.SECONDARY, Trig.Historic.fromCode("2"));
        assertEquals(Trig.Historic.THIRDORDER, Trig.Historic.fromCode("3"));
        assertEquals(Trig.Historic.PASSIVE, Trig.Historic.fromCode("P"));
        
        // Test unknown code
        assertEquals(Trig.Historic.UNKNOWN, Trig.Historic.fromCode("UNKNOWN"));
        assertEquals(Trig.Historic.UNKNOWN, Trig.Historic.fromCode(null));
    }

    @Test
    public void testTrigInheritsFromLatLon() {
        // Verify Trig extends LatLon
        assertTrue(testTrig instanceof LatLon);
        
        // Test that LatLon methods work
        testTrig.setLat(54.123);
        testTrig.setLon(-2.456);
        assertEquals(54.123, testTrig.getLat(), 0.001);
        assertEquals(-2.456, testTrig.getLon(), 0.001);
    }

    @Test
    public void testSerializable() {
        // Test that the class has serialVersionUID (indicates Serializable)
        try {
            java.lang.reflect.Field field = Trig.class.getDeclaredField("serialVersionUID");
            field.setAccessible(true);
            assertEquals(1L, field.getLong(null));
        } catch (Exception e) {
            fail("serialVersionUID field should be accessible");
        }
    }
}

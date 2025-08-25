package uk.trigpointing.android.types;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.trigpointing.android.R;

/**
 * Unit tests for Condition enum
 * Testing condition codes, icons, and business logic
 */
public class ConditionTest {

    @Test
    public void testConditionEnumValues() {
        // Test key condition values
        assertNotNull(Condition.GOOD);
        assertEquals("G", Condition.GOOD.code());
        assertEquals("Good", Condition.GOOD.toString());
        assertEquals(R.drawable.c_good, Condition.GOOD.icon());
        assertEquals(R.drawable.cs_good, Condition.GOOD.icon(true));
        assertEquals(R.drawable.c_good, Condition.GOOD.icon(false));

        assertNotNull(Condition.DAMAGED);
        assertEquals("D", Condition.DAMAGED.code());
        assertEquals("Damaged", Condition.DAMAGED.toString());
        assertEquals(R.drawable.c_damaged, Condition.DAMAGED.icon());

        assertNotNull(Condition.MISSING);
        assertEquals("X", Condition.MISSING.code());
        assertEquals("Destroyed", Condition.MISSING.toString());
        assertEquals(R.drawable.c_definitelymissing, Condition.MISSING.icon());

        assertNotNull(Condition.TOPPLED);
        assertEquals("T", Condition.TOPPLED.code());
        assertEquals("Toppled", Condition.TOPPLED.toString());
        assertEquals(R.drawable.c_toppled, Condition.TOPPLED.icon());
    }

    @Test
    public void testFromCodeValidCodes() {
        assertEquals(Condition.GOOD, Condition.fromCode("G"));
        assertEquals(Condition.DAMAGED, Condition.fromCode("D"));
        assertEquals(Condition.MISSING, Condition.fromCode("X"));
        assertEquals(Condition.TOPPLED, Condition.fromCode("T"));
        assertEquals(Condition.SLIGHTLYDAMAGED, Condition.fromCode("S"));
        assertEquals(Condition.CONVERTED, Condition.fromCode("C"));
        assertEquals(Condition.REMAINS, Condition.fromCode("R"));
        assertEquals(Condition.MOVED, Condition.fromCode("M"));
        assertEquals(Condition.POSSIBLYMISSING, Condition.fromCode("Q"));
        assertEquals(Condition.VISIBLE, Condition.fromCode("V"));
        assertEquals(Condition.INACCESSIBLE, Condition.fromCode("P"));
        assertEquals(Condition.COULDNTFIND, Condition.fromCode("N"));
        assertEquals(Condition.CONDITIONNOTLOGGED, Condition.fromCode("Z"));
        assertEquals(Condition.UNKNOWN, Condition.fromCode("U"));
        assertEquals(Condition.TRIGNOTLOGGED, Condition.fromCode("-"));
    }

    @Test
    public void testFromCodeInvalidCodes() {
        // Test invalid/unknown codes return UNKNOWN
        assertEquals(Condition.UNKNOWN, Condition.fromCode("INVALID"));
        assertEquals(Condition.UNKNOWN, Condition.fromCode(""));
        assertEquals(Condition.UNKNOWN, Condition.fromCode(null));
        assertEquals(Condition.UNKNOWN, Condition.fromCode("123"));
    }

    @Test
    public void testIconWithHighlight() {
        Condition condition = Condition.GOOD;
        
        // Test highlighted icon
        assertEquals(R.drawable.cs_good, condition.icon(true));
        
        // Test normal icon
        assertEquals(R.drawable.c_good, condition.icon(false));
        
        // Test that different conditions have different highlighted icons
        assertNotEquals(condition.icon(true), Condition.DAMAGED.icon(true));
    }

    @Test
    public void testAllConditionsHaveValidProperties() {
        // Ensure all enum values have non-null properties
        for (Condition condition : Condition.values()) {
            assertNotNull("Code should not be null for " + condition, condition.code());
            assertNotNull("Description should not be null for " + condition, condition.toString());
            assertTrue("Icon resource should be positive for " + condition, condition.icon() > 0);
            assertTrue("Highlighted icon resource should be positive for " + condition, condition.icon(true) > 0);
        }
    }

    @Test
    public void testConditionCodesAreUnique() {
        // Check that all condition codes are unique
        String[] codes = new String[Condition.values().length];
        for (int i = 0; i < Condition.values().length; i++) {
            codes[i] = Condition.values()[i].code();
        }
        
        // Check for duplicates
        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals("Duplicate condition code found: " + codes[i], codes[i], codes[j]);
            }
        }
    }

    @Test
    public void testSpecificConditionLogic() {
        // Test that conditions representing different severity levels exist
        assertTrue("Should have a good condition", Condition.GOOD.toString().contains("Good"));
        assertTrue("Should have a damaged condition", Condition.DAMAGED.toString().contains("Damaged"));
        assertTrue("Should have a missing condition", Condition.MISSING.toString().contains("Destroyed"));
        assertTrue("Should have not logged condition", Condition.TRIGNOTLOGGED.toString().contains("Not Visited"));
    }
}

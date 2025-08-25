package uk.trigpointing.android.types;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PhotoSubject enum
 * Testing photo subject codes and descriptions
 */
public class PhotoSubjectTest {

    @Test
    public void testPhotoSubjectEnumValues() {
        // Test key photo subject values
        assertNotNull(PhotoSubject.TRIGPOINT);
        assertEquals("T", PhotoSubject.TRIGPOINT.code());
        assertEquals("The trigpoint", PhotoSubject.TRIGPOINT.toString());

        assertNotNull(PhotoSubject.FLUSHBRACKET);
        assertEquals("F", PhotoSubject.FLUSHBRACKET.code());
        assertEquals("The flush bracket", PhotoSubject.FLUSHBRACKET.toString());

        assertNotNull(PhotoSubject.PEOPLE);
        assertEquals("P", PhotoSubject.PEOPLE.code());
        assertEquals("People", PhotoSubject.PEOPLE.toString());

        assertNotNull(PhotoSubject.LANDSCAPE);
        assertEquals("L", PhotoSubject.LANDSCAPE.code());
        assertEquals("Landscape", PhotoSubject.LANDSCAPE.toString());

        assertNotNull(PhotoSubject.OTHER);
        assertEquals("O", PhotoSubject.OTHER.code());
        assertEquals("Other", PhotoSubject.OTHER.toString());

        assertNotNull(PhotoSubject.NOSUBJECT);
        assertEquals(" ", PhotoSubject.NOSUBJECT.code());
        assertEquals("", PhotoSubject.NOSUBJECT.toString());
    }

    @Test
    public void testFromCodeValidCodes() {
        assertEquals(PhotoSubject.TRIGPOINT, PhotoSubject.fromCode("T"));
        assertEquals(PhotoSubject.FLUSHBRACKET, PhotoSubject.fromCode("F"));
        assertEquals(PhotoSubject.PEOPLE, PhotoSubject.fromCode("P"));
        assertEquals(PhotoSubject.LANDSCAPE, PhotoSubject.fromCode("L"));
        assertEquals(PhotoSubject.OTHER, PhotoSubject.fromCode("O"));
        assertEquals(PhotoSubject.NOSUBJECT, PhotoSubject.fromCode(" "));
    }

    @Test
    public void testFromCodeInvalidCodes() {
        // Test invalid/unknown codes return NOSUBJECT
        assertEquals(PhotoSubject.NOSUBJECT, PhotoSubject.fromCode("INVALID"));
        assertEquals(PhotoSubject.NOSUBJECT, PhotoSubject.fromCode(""));
        assertEquals(PhotoSubject.NOSUBJECT, PhotoSubject.fromCode(null));
        assertEquals(PhotoSubject.NOSUBJECT, PhotoSubject.fromCode("123"));
        assertEquals(PhotoSubject.NOSUBJECT, PhotoSubject.fromCode("X"));
    }

    @Test
    public void testAllPhotoSubjectsHaveValidProperties() {
        // Ensure all enum values have non-null properties
        for (PhotoSubject subject : PhotoSubject.values()) {
            assertNotNull("Code should not be null for " + subject, subject.code());
            assertNotNull("Description should not be null for " + subject, subject.toString());
        }
    }

    @Test
    public void testPhotoSubjectCodesAreUnique() {
        // Check that all photo subject codes are unique
        String[] codes = new String[PhotoSubject.values().length];
        for (int i = 0; i < PhotoSubject.values().length; i++) {
            codes[i] = PhotoSubject.values()[i].code();
        }
        
        // Check for duplicates
        for (int i = 0; i < codes.length; i++) {
            for (int j = i + 1; j < codes.length; j++) {
                assertNotEquals("Duplicate photo subject code found: " + codes[i], codes[i], codes[j]);
            }
        }
    }

    @Test
    public void testSpecificPhotoSubjectLogic() {
        // Test that important photo subjects exist and make sense
        assertTrue("Trigpoint subject should refer to trigpoint", 
                  PhotoSubject.TRIGPOINT.toString().toLowerCase().contains("trigpoint"));
        assertTrue("People subject should refer to people", 
                  PhotoSubject.PEOPLE.toString().toLowerCase().contains("people"));
        assertTrue("Landscape subject should refer to landscape", 
                  PhotoSubject.LANDSCAPE.toString().toLowerCase().contains("landscape"));
        assertTrue("No subject should have empty description", 
                  PhotoSubject.NOSUBJECT.toString().isEmpty());
    }
}

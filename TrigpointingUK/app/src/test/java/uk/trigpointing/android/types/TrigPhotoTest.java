package uk.trigpointing.android.types;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for TrigPhoto class
 * Testing trigpoint photo data structure and properties
 */
public class TrigPhotoTest {

    private TrigPhoto testTrigPhoto;

    @Before
    public void setUp() {
        testTrigPhoto = new TrigPhoto("Test Photo", "A test photo description", 
                                     "https://example.com/photo.jpg", "https://example.com/icon.jpg",
                                     "testuser", "2023-08-15");
    }

    @Test
    public void testTrigPhotoConstructorWithAllParameters() {
        assertNotNull("TrigPhoto should be created", testTrigPhoto);
        assertEquals("Name should be set", "Test Photo", testTrigPhoto.getName());
        assertEquals("Description should be set", "A test photo description", testTrigPhoto.getDescr());
        assertEquals("Photo URL should be set", "https://example.com/photo.jpg", testTrigPhoto.getPhotoURL());
        assertEquals("Icon URL should be set", "https://example.com/icon.jpg", testTrigPhoto.getIconURL());
        assertEquals("Username should be set", "testuser", testTrigPhoto.getUsername());
        assertEquals("Date should be set", "2023-08-15", testTrigPhoto.getDate());
    }

    @Test
    public void testTrigPhotoDefaultConstructor() {
        TrigPhoto emptyPhoto = new TrigPhoto();
        assertNotNull("TrigPhoto should be created with default constructor", emptyPhoto);
        assertNull("Name should be null", emptyPhoto.getName());
        assertNull("Description should be null", emptyPhoto.getDescr());
        assertNull("Photo URL should be null", emptyPhoto.getPhotoURL());
        assertNull("Icon URL should be null", emptyPhoto.getIconURL());
        assertNull("Username should be null", emptyPhoto.getUsername());
        assertNull("Date should be null", emptyPhoto.getDate());
        assertNull("Subject should be null", emptyPhoto.getSubject());
        assertNull("Is public should be null", emptyPhoto.getIspublic());
        assertNull("Log ID should be null", emptyPhoto.getLogID());
    }

    @Test
    public void testNameProperty() {
        String newName = "Updated Photo Name";
        testTrigPhoto.setName(newName);
        assertEquals("Name should be updated", newName, testTrigPhoto.getName());
    }

    @Test
    public void testDescrProperty() {
        String newDescription = "Updated photo description";
        testTrigPhoto.setDescr(newDescription);
        assertEquals("Description should be updated", newDescription, testTrigPhoto.getDescr());
    }

    @Test
    public void testPhotoURLProperty() {
        String newPhotoURL = "https://example.com/newphoto.jpg";
        testTrigPhoto.setPhotoURL(newPhotoURL);
        assertEquals("Photo URL should be updated", newPhotoURL, testTrigPhoto.getPhotoURL());
    }

    @Test
    public void testIconURLProperty() {
        String newIconURL = "https://example.com/newicon.jpg";
        testTrigPhoto.setIconURL(newIconURL);
        assertEquals("Icon URL should be updated", newIconURL, testTrigPhoto.getIconURL());
    }

    @Test
    public void testUsernameProperty() {
        String newUsername = "newuser";
        testTrigPhoto.setUsername(newUsername);
        assertEquals("Username should be updated", newUsername, testTrigPhoto.getUsername());
    }

    @Test
    public void testDateProperty() {
        String newDate = "2024-01-01";
        testTrigPhoto.setDate(newDate);
        assertEquals("Date should be updated", newDate, testTrigPhoto.getDate());
    }

    @Test
    public void testSubjectProperty() {
        String testSubject = "Trigpoint";
        testTrigPhoto.setSubject(testSubject);
        assertEquals("Subject should be set", testSubject, testTrigPhoto.getSubject());
    }

    @Test
    public void testIsPublicProperty() {
        Boolean isPublic = true;
        testTrigPhoto.setIspublic(isPublic);
        assertEquals("Is public should be set", isPublic, testTrigPhoto.getIspublic());
        
        isPublic = false;
        testTrigPhoto.setIspublic(isPublic);
        assertEquals("Is public should be updated", isPublic, testTrigPhoto.getIspublic());
    }

    @Test
    public void testLogIDProperty() {
        Long testLogID = 12345L;
        testTrigPhoto.setLogID(testLogID);
        assertEquals("Log ID should be set", testLogID, testTrigPhoto.getLogID());
    }

    @Test
    public void testTrigPhotoWithNullValues() {
        TrigPhoto nullPhoto = new TrigPhoto(null, null, null, null, null, null);
        assertNull("Name can be null", nullPhoto.getName());
        assertNull("Description can be null", nullPhoto.getDescr());
        assertNull("Photo URL can be null", nullPhoto.getPhotoURL());
        assertNull("Icon URL can be null", nullPhoto.getIconURL());
        assertNull("Username can be null", nullPhoto.getUsername());
        assertNull("Date can be null", nullPhoto.getDate());
    }

    @Test
    public void testTrigPhotoWithEmptyStrings() {
        TrigPhoto emptyPhoto = new TrigPhoto("", "", "", "", "", "");
        assertEquals("Name can be empty", "", emptyPhoto.getName());
        assertEquals("Description can be empty", "", emptyPhoto.getDescr());
        assertEquals("Photo URL can be empty", "", emptyPhoto.getPhotoURL());
        assertEquals("Icon URL can be empty", "", emptyPhoto.getIconURL());
        assertEquals("Username can be empty", "", emptyPhoto.getUsername());
        assertEquals("Date can be empty", "", emptyPhoto.getDate());
    }

    @Test
    public void testCompletePhotoWithAllProperties() {
        TrigPhoto completePhoto = new TrigPhoto();
        
        // Set all properties
        completePhoto.setName("Complete Photo");
        completePhoto.setDescr("A complete photo with all properties");
        completePhoto.setPhotoURL("https://example.com/complete.jpg");
        completePhoto.setIconURL("https://example.com/complete_icon.jpg");
        completePhoto.setUsername("completeuser");
        completePhoto.setDate("2023-12-25");
        completePhoto.setSubject("Landscape");
        completePhoto.setIspublic(true);
        completePhoto.setLogID(98765L);

        // Verify all properties
        assertEquals("Complete name", "Complete Photo", completePhoto.getName());
        assertEquals("Complete description", "A complete photo with all properties", completePhoto.getDescr());
        assertEquals("Complete photo URL", "https://example.com/complete.jpg", completePhoto.getPhotoURL());
        assertEquals("Complete icon URL", "https://example.com/complete_icon.jpg", completePhoto.getIconURL());
        assertEquals("Complete username", "completeuser", completePhoto.getUsername());
        assertEquals("Complete date", "2023-12-25", completePhoto.getDate());
        assertEquals("Complete subject", "Landscape", completePhoto.getSubject());
        assertEquals("Complete is public", Boolean.TRUE, completePhoto.getIspublic());
        assertEquals("Complete log ID", Long.valueOf(98765L), completePhoto.getLogID());
    }

    @Test
    public void testTrigPhotoConstructorParameterOrder() {
        // Test that constructor parameters are in the expected order: name, descr, photourl, iconurl, username, date
        TrigPhoto photo = new TrigPhoto("param1", "param2", "param3", "param4", "param5", "param6");
        
        assertEquals("First parameter should be name", "param1", photo.getName());
        assertEquals("Second parameter should be description", "param2", photo.getDescr());
        assertEquals("Third parameter should be photo URL", "param3", photo.getPhotoURL());
        assertEquals("Fourth parameter should be icon URL", "param4", photo.getIconURL());
        assertEquals("Fifth parameter should be username", "param5", photo.getUsername());
        assertEquals("Sixth parameter should be date", "param6", photo.getDate());
    }

    @Test
    public void testPhotoURLHandling() {
        // Test various URL formats
        String[] testURLs = {
            "https://example.com/photo.jpg",
            "http://example.com/photo.png",
            "ftp://example.com/photo.gif",
            "file:///storage/photo.jpeg",
            "content://media/external/photo.bmp"
        };
        
        for (String url : testURLs) {
            testTrigPhoto.setPhotoURL(url);
            assertEquals("Photo URL should handle format: " + url, url, testTrigPhoto.getPhotoURL());
        }
    }

    @Test
    public void testBooleanStateHandling() {
        // Test Boolean state transitions for isPublic
        assertNull("Initial is public should be null", testTrigPhoto.getIspublic());
        
        testTrigPhoto.setIspublic(true);
        assertEquals("Should be true", Boolean.TRUE, testTrigPhoto.getIspublic());
        
        testTrigPhoto.setIspublic(false);
        assertEquals("Should be false", Boolean.FALSE, testTrigPhoto.getIspublic());
        
        testTrigPhoto.setIspublic(null);
        assertNull("Should be null again", testTrigPhoto.getIspublic());
    }

    @Test
    public void testLogIDVariations() {
        // Test various log ID values
        Long[] testLogIDs = {0L, 1L, 999999L, Long.MAX_VALUE, Long.MIN_VALUE};
        
        for (Long logID : testLogIDs) {
            testTrigPhoto.setLogID(logID);
            assertEquals("Log ID should handle value: " + logID, logID, testTrigPhoto.getLogID());
        }
        
        testTrigPhoto.setLogID(null);
        assertNull("Log ID should handle null", testTrigPhoto.getLogID());
    }

    @Test
    public void testDataIntegrity() {
        // Test that setting one property doesn't affect others
        testTrigPhoto.setName("New Name");
        assertEquals("Original description should be preserved", "A test photo description", testTrigPhoto.getDescr());
        assertEquals("Original photo URL should be preserved", "https://example.com/photo.jpg", testTrigPhoto.getPhotoURL());
        assertEquals("Original username should be preserved", "testuser", testTrigPhoto.getUsername());
        
        testTrigPhoto.setSubject("Test Subject");
        assertEquals("Name should still be new value", "New Name", testTrigPhoto.getName());
        assertEquals("Subject should be set", "Test Subject", testTrigPhoto.getSubject());
    }
}

package uk.trigpointing.android.api;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for User data model class
 * Testing user data properties and behavior
 */
public class UserTest {

    private User testUser;

    @Before
    public void setUp() {
        testUser = new User();
    }

    @Test
    public void testUserCreation() {
        assertNotNull("User should be created", testUser);
        assertEquals("Initial ID should be 0", 0, testUser.getId());
        assertNull("Initial name should be null", testUser.getName());
        assertNull("Initial email should be null", testUser.getEmail());
    }

    @Test
    public void testUserIdProperty() {
        int testId = 12345;
        testUser.setId(testId);
        assertEquals("User ID should be set and retrieved correctly", testId, testUser.getId());
    }

    @Test
    public void testUserNameProperty() {
        String testName = "John Doe";
        testUser.setName(testName);
        assertEquals("User name should be set and retrieved correctly", testName, testUser.getName());
    }

    @Test
    public void testFirstnameProperty() {
        String testFirstname = "John";
        testUser.setFirstname(testFirstname);
        assertEquals("Firstname should be set and retrieved correctly", testFirstname, testUser.getFirstname());
    }

    @Test
    public void testSurnameProperty() {
        String testSurname = "Doe";
        testUser.setSurname(testSurname);
        assertEquals("Surname should be set and retrieved correctly", testSurname, testUser.getSurname());
    }

    @Test
    public void testAboutProperty() {
        String testAbout = "A passionate trigpoint hunter from the UK.";
        testUser.setAbout(testAbout);
        assertEquals("About should be set and retrieved correctly", testAbout, testUser.getAbout());
    }

    @Test
    public void testEmailProperty() {
        String testEmail = "john.doe@example.com";
        testUser.setEmail(testEmail);
        assertEquals("Email should be set and retrieved correctly", testEmail, testUser.getEmail());
    }

    @Test
    public void testEmailValidProperty() {
        String testEmailValid = "Y";
        testUser.setEmailValid(testEmailValid);
        assertEquals("Email valid should be set and retrieved correctly", testEmailValid, testUser.getEmailValid());
    }

    @Test
    public void testAdminIndProperty() {
        String testAdminInd = "Y";
        testUser.setAdminInd(testAdminInd);
        assertEquals("Admin indicator should be set and retrieved correctly", testAdminInd, testUser.getAdminInd());
    }

    @Test
    public void testPublicIndProperty() {
        String testPublicInd = "N";
        testUser.setPublicInd(testPublicInd);
        assertEquals("Public indicator should be set and retrieved correctly", testPublicInd, testUser.getPublicInd());
    }

    @Test
    public void testUserWithAllProperties() {
        // Set all properties
        testUser.setId(456);
        testUser.setName("Jane Smith");
        testUser.setFirstname("Jane");
        testUser.setSurname("Smith");
        testUser.setAbout("An experienced trigpoint photographer.");
        testUser.setEmail("jane.smith@example.com");
        testUser.setEmailValid("Y");
        testUser.setAdminInd("N");
        testUser.setPublicInd("Y");

        // Verify all properties
        assertEquals("ID should be set", 456, testUser.getId());
        assertEquals("Name should be set", "Jane Smith", testUser.getName());
        assertEquals("Firstname should be set", "Jane", testUser.getFirstname());
        assertEquals("Surname should be set", "Smith", testUser.getSurname());
        assertEquals("About should be set", "An experienced trigpoint photographer.", testUser.getAbout());
        assertEquals("Email should be set", "jane.smith@example.com", testUser.getEmail());
        assertEquals("Email valid should be set", "Y", testUser.getEmailValid());
        assertEquals("Admin indicator should be set", "N", testUser.getAdminInd());
        assertEquals("Public indicator should be set", "Y", testUser.getPublicInd());
    }

    @Test
    public void testUserToString() {
        testUser.setId(123);
        testUser.setName("Test User");
        testUser.setFirstname("Test");
        testUser.setSurname("User");
        testUser.setEmail("test@example.com");
        testUser.setAdminInd("Y");

        String stringRepresentation = testUser.toString();
        
        assertNotNull("toString should not return null", stringRepresentation);
        assertTrue("toString should contain class name", stringRepresentation.contains("User{"));
        assertTrue("toString should contain ID", stringRepresentation.contains("id=123"));
        assertTrue("toString should contain name", stringRepresentation.contains("name='Test User'"));
        assertTrue("toString should contain firstname", stringRepresentation.contains("firstname='Test'"));
        assertTrue("toString should contain surname", stringRepresentation.contains("surname='User'"));
        assertTrue("toString should contain email", stringRepresentation.contains("email='test@example.com'"));
        assertTrue("toString should contain admin indicator", stringRepresentation.contains("admin_ind='Y'"));
    }

    @Test
    public void testUserWithNullValues() {
        // Test that the user can handle null values gracefully
        testUser.setName(null);
        testUser.setEmail(null);
        testUser.setAbout(null);

        assertNull("Name should be null", testUser.getName());
        assertNull("Email should be null", testUser.getEmail());
        assertNull("About should be null", testUser.getAbout());

        // toString should handle null values
        String stringRepresentation = testUser.toString();
        assertNotNull("toString should not return null even with null properties", stringRepresentation);
    }

    @Test
    public void testUserWithEmptyStrings() {
        // Test that the user can handle empty strings
        testUser.setName("");
        testUser.setEmail("");
        testUser.setAbout("");
        testUser.setFirstname("");
        testUser.setSurname("");

        assertEquals("Name should be empty string", "", testUser.getName());
        assertEquals("Email should be empty string", "", testUser.getEmail());
        assertEquals("About should be empty string", "", testUser.getAbout());
        assertEquals("Firstname should be empty string", "", testUser.getFirstname());
        assertEquals("Surname should be empty string", "", testUser.getSurname());
    }

    @Test
    public void testUserIndicatorValues() {
        // Test boolean-like string indicators
        testUser.setEmailValid("Y");
        testUser.setAdminInd("N");
        testUser.setPublicInd("Y");

        assertEquals("Email valid should be Y", "Y", testUser.getEmailValid());
        assertEquals("Admin indicator should be N", "N", testUser.getAdminInd());
        assertEquals("Public indicator should be Y", "Y", testUser.getPublicInd());

        // Test with other values
        testUser.setEmailValid("N");
        testUser.setAdminInd("Y");
        testUser.setPublicInd("N");

        assertEquals("Email valid should be N", "N", testUser.getEmailValid());
        assertEquals("Admin indicator should be Y", "Y", testUser.getAdminInd());
        assertEquals("Public indicator should be N", "N", testUser.getPublicInd());
    }

    @Test
    public void testDefaultConstructor() {
        User newUser = new User();
        assertNotNull("Default constructor should create user", newUser);
        assertEquals("Default ID should be 0", 0, newUser.getId());
        assertNull("Default name should be null", newUser.getName());
        assertNull("Default email should be null", newUser.getEmail());
    }
}

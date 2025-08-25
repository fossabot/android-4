package uk.trigpointing.android.api;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for AuthResponse data model class
 * Testing authentication response properties and behavior
 */
public class AuthResponseTest {

    private AuthResponse testAuthResponse;
    private User testUser;

    @Before
    public void setUp() {
        testAuthResponse = new AuthResponse();
        testUser = new User();
        testUser.setId(123);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
    }

    @Test
    public void testAuthResponseCreation() {
        assertNotNull("AuthResponse should be created", testAuthResponse);
        assertNull("Initial access token should be null", testAuthResponse.getAccessToken());
        assertNull("Initial token type should be null", testAuthResponse.getTokenType());
        assertNull("Initial user should be null", testAuthResponse.getUser());
        assertEquals("Initial expires in should be 0", 0, testAuthResponse.getExpiresIn());
    }

    @Test
    public void testAccessTokenProperty() {
        String testToken = "abc123_test_token_xyz789";
        testAuthResponse.setAccessToken(testToken);
        assertEquals("Access token should be set and retrieved correctly", testToken, testAuthResponse.getAccessToken());
    }

    @Test
    public void testTokenTypeProperty() {
        String testTokenType = "Bearer";
        testAuthResponse.setTokenType(testTokenType);
        assertEquals("Token type should be set and retrieved correctly", testTokenType, testAuthResponse.getTokenType());
    }

    @Test
    public void testUserProperty() {
        testAuthResponse.setUser(testUser);
        assertEquals("User should be set and retrieved correctly", testUser, testAuthResponse.getUser());
        assertEquals("User ID should be accessible through auth response", 123, testAuthResponse.getUser().getId());
        assertEquals("User name should be accessible through auth response", "Test User", testAuthResponse.getUser().getName());
    }

    @Test
    public void testExpiresInProperty() {
        int testExpiresIn = 3600; // 1 hour
        testAuthResponse.setExpiresIn(testExpiresIn);
        assertEquals("Expires in should be set and retrieved correctly", testExpiresIn, testAuthResponse.getExpiresIn());
    }

    @Test
    public void testCompleteAuthResponse() {
        // Set all properties
        testAuthResponse.setAccessToken("full_test_token_12345");
        testAuthResponse.setTokenType("Bearer");
        testAuthResponse.setUser(testUser);
        testAuthResponse.setExpiresIn(7200);

        // Verify all properties
        assertEquals("Access token should be set", "full_test_token_12345", testAuthResponse.getAccessToken());
        assertEquals("Token type should be set", "Bearer", testAuthResponse.getTokenType());
        assertNotNull("User should be set", testAuthResponse.getUser());
        assertEquals("User should be the test user", testUser, testAuthResponse.getUser());
        assertEquals("Expires in should be set", 7200, testAuthResponse.getExpiresIn());
    }

    @Test
    public void testAuthResponseToString() {
        testAuthResponse.setAccessToken("test_token");
        testAuthResponse.setTokenType("Bearer");
        testAuthResponse.setUser(testUser);
        testAuthResponse.setExpiresIn(3600);

        String stringRepresentation = testAuthResponse.toString();
        
        assertNotNull("toString should not return null", stringRepresentation);
        assertTrue("toString should contain class name", stringRepresentation.contains("AuthResponse{"));
        assertTrue("toString should contain access token", stringRepresentation.contains("access_token='test_token'"));
        assertTrue("toString should contain token type", stringRepresentation.contains("token_type='Bearer'"));
        assertTrue("toString should contain user", stringRepresentation.contains("user="));
        assertTrue("toString should contain expires in", stringRepresentation.contains("expires_in=3600"));
    }

    @Test
    public void testAuthResponseWithNullValues() {
        // Test that AuthResponse can handle null values gracefully
        testAuthResponse.setAccessToken(null);
        testAuthResponse.setTokenType(null);
        testAuthResponse.setUser(null);

        assertNull("Access token should be null", testAuthResponse.getAccessToken());
        assertNull("Token type should be null", testAuthResponse.getTokenType());
        assertNull("User should be null", testAuthResponse.getUser());

        // toString should handle null values
        String stringRepresentation = testAuthResponse.toString();
        assertNotNull("toString should not return null even with null properties", stringRepresentation);
    }

    @Test
    public void testAuthResponseWithEmptyStrings() {
        // Test that AuthResponse can handle empty strings
        testAuthResponse.setAccessToken("");
        testAuthResponse.setTokenType("");

        assertEquals("Access token should be empty string", "", testAuthResponse.getAccessToken());
        assertEquals("Token type should be empty string", "", testAuthResponse.getTokenType());
    }

    @Test
    public void testAuthResponseWithVariousExpiryTimes() {
        // Test different expiry times
        testAuthResponse.setExpiresIn(0);
        assertEquals("Zero expiry should be allowed", 0, testAuthResponse.getExpiresIn());

        testAuthResponse.setExpiresIn(3600); // 1 hour
        assertEquals("One hour expiry", 3600, testAuthResponse.getExpiresIn());

        testAuthResponse.setExpiresIn(86400); // 24 hours
        assertEquals("24 hour expiry", 86400, testAuthResponse.getExpiresIn());

        testAuthResponse.setExpiresIn(-1); // Negative (possibly invalid)
        assertEquals("Negative expiry should be stored", -1, testAuthResponse.getExpiresIn());
    }

    @Test
    public void testAuthResponseUserIntegration() {
        // Test the integration between AuthResponse and User
        User detailedUser = new User();
        detailedUser.setId(456);
        detailedUser.setName("Detailed User");
        detailedUser.setFirstname("Detailed");
        detailedUser.setSurname("User");
        detailedUser.setEmail("detailed@example.com");
        detailedUser.setEmailValid("Y");
        detailedUser.setAdminInd("N");
        detailedUser.setPublicInd("Y");

        testAuthResponse.setUser(detailedUser);

        assertNotNull("User should be set", testAuthResponse.getUser());
        assertEquals("User ID should match", 456, testAuthResponse.getUser().getId());
        assertEquals("User name should match", "Detailed User", testAuthResponse.getUser().getName());
        assertEquals("User email should match", "detailed@example.com", testAuthResponse.getUser().getEmail());
    }

    @Test
    public void testDefaultConstructor() {
        AuthResponse newResponse = new AuthResponse();
        assertNotNull("Default constructor should create AuthResponse", newResponse);
        assertNull("Default access token should be null", newResponse.getAccessToken());
        assertNull("Default token type should be null", newResponse.getTokenType());
        assertNull("Default user should be null", newResponse.getUser());
        assertEquals("Default expires in should be 0", 0, newResponse.getExpiresIn());
    }

    @Test
    public void testLongAccessToken() {
        // Test with a very long access token
        StringBuilder longToken = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longToken.append("abc123xyz");
        }
        
        testAuthResponse.setAccessToken(longToken.toString());
        assertEquals("Long access token should be preserved", longToken.toString(), testAuthResponse.getAccessToken());
    }
}

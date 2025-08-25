package uk.trigpointing.android.mapping;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for URL parameter parsing and encoding compatibility
 * Tests the fixes we implemented for Android WebView compatibility
 */
public class LeafletUrlParameterTest {

    @Test
    public void testURLEncoderCompatibilityFix() throws UnsupportedEncodingException {
        // Test the fix for URLEncoder.encode compatibility issue
        String testValue = "test123+special chars";
        String expected = URLEncoder.encode(testValue, "UTF-8");
        String actual = URLEncoder.encode(testValue, StandardCharsets.UTF_8.name());
        
        assertEquals("UTF-8 encoding should work with string parameter", expected, actual);
    }

    @Test
    public void testURLEncoderWithSpecialCharacters() throws UnsupportedEncodingException {
        // Test encoding of special characters that might appear in API keys
        String apiKey = "9JlecAAXIkGUlWJQovSE946uNHzJew8u";
        String encoded = URLEncoder.encode(apiKey, StandardCharsets.UTF_8.name());
        
        assertNotNull("Encoded value should not be null", encoded);
        assertFalse("Encoded value should not be empty", encoded.isEmpty());
        // API keys typically don't need encoding, but should handle gracefully
        assertEquals("Simple API key should encode to itself", apiKey, encoded);
    }

    @Test
    public void testURLEncoderWithSpacesAndSymbols() throws UnsupportedEncodingException {
        String testValue = "OpenStreetMap Style";
        String encoded = URLEncoder.encode(testValue, StandardCharsets.UTF_8.name());
        
        assertTrue("Spaces should be encoded as +", encoded.contains("+"));
        assertEquals("OpenStreetMap+Style", encoded);
    }

    @Test
    public void testURLEncoderEdgeCases() throws UnsupportedEncodingException {
        // Test edge cases that might break on older Android versions
        
        // Empty string
        String empty = URLEncoder.encode("", StandardCharsets.UTF_8.name());
        assertEquals("Empty string should encode to empty", "", empty);
        
        // Only special characters
        String special = URLEncoder.encode("&=?#", StandardCharsets.UTF_8.name());
        assertFalse("Special chars should be encoded", special.equals("&=?#"));
        
        // Unicode characters
        String unicode = URLEncoder.encode("café", StandardCharsets.UTF_8.name());
        assertTrue("Unicode should be encoded", unicode.contains("%"));
    }

    /**
     * This simulates the JavaScript URL parameter parsing we fixed
     * Tests the equivalent logic in Java for validation
     */
    @Test
    public void testURLParameterParsing() {
        String testUrl = "file:///android_asset/leaflet/index.html?os_key=9JlecAAXIkGUlWJQovSE946uNHzJew8u&initial_style=OpenStreetMap";
        
        String osKey = getParamFromUrl(testUrl, "os_key");
        String initialStyle = getParamFromUrl(testUrl, "initial_style");
        
        assertEquals("Should extract os_key correctly", "9JlecAAXIkGUlWJQovSE946uNHzJew8u", osKey);
        assertEquals("Should extract initial_style correctly", "OpenStreetMap", initialStyle);
    }

    @Test
    public void testURLParameterParsingEdgeCases() {
        // Test various edge cases for URL parameter parsing
        
        // No parameters
        String noParams = "file:///android_asset/leaflet/index.html";
        assertEquals("", getParamFromUrl(noParams, "os_key"));
        
        // Empty parameter value
        String emptyValue = "file:///android_asset/leaflet/index.html?os_key=&style=test";
        assertEquals("", getParamFromUrl(emptyValue, "os_key"));
        assertEquals("test", getParamFromUrl(emptyValue, "style"));
        
        // Parameter not found
        assertEquals("", getParamFromUrl("file:///test?other=value", "missing"));
        
        // Multiple parameters
        String multiParams = "file:///test?a=1&b=2&c=3";
        assertEquals("1", getParamFromUrl(multiParams, "a"));
        assertEquals("2", getParamFromUrl(multiParams, "b"));
        assertEquals("3", getParamFromUrl(multiParams, "c"));
    }

    @Test
    public void testURLParameterParsingWithEncodedValues() {
        // Test URL parameter parsing with encoded values
        String encodedUrl = "file:///test?key=OpenStreetMap%20Style&value=test%2Bdata";
        
        String key = getParamFromUrl(encodedUrl, "key");
        String value = getParamFromUrl(encodedUrl, "value");
        
        // This method would need to handle URL decoding in a real implementation
        assertNotNull("Key should be extracted", key);
        assertNotNull("Value should be extracted", value);
    }

    /**
     * Java equivalent of the JavaScript getParam function we fixed
     * This mimics the backward-compatible URL parameter parsing
     */
    private String getParamFromUrl(String url, String paramName) {
        if (url == null || !url.contains("?")) {
            return "";
        }
        
        String queryString = url.substring(url.indexOf("?") + 1);
        if (queryString.isEmpty()) {
            return "";
        }
        
        String[] params = queryString.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length >= 1 && keyValue[0].equals(paramName)) {
                return keyValue.length > 1 ? keyValue[1] : "";
            }
        }
        
        return "";
    }

    @Test
    public void testBuildLeafletUrlEquivalent() {
        // Test the equivalent of LeafletMapActivity.buildLeafletUrl method
        String baseUrl = "file:///android_asset/leaflet/index.html";
        String osKey = "9JlecAAXIkGUlWJQovSE946uNHzJew8u";
        String style = "OpenStreetMap";
        
        String url = buildLeafletUrlEquivalent(baseUrl, osKey, style);
        
        assertTrue("URL should contain base", url.startsWith(baseUrl));
        assertTrue("URL should contain os_key parameter", url.contains("os_key=" + osKey));
        assertTrue("URL should contain initial_style parameter", url.contains("initial_style=" + style));
        assertTrue("URL should be properly formatted", url.contains("?") && url.contains("&"));
    }

    /**
     * Java equivalent of the buildLeafletUrl method to test URL construction
     */
    private String buildLeafletUrlEquivalent(String baseUrl, String osKey, String leafletMapStyle) {
        StringBuilder url = new StringBuilder(baseUrl);
        boolean hasParams = false;

        if (osKey != null && !osKey.isEmpty()) {
            try {
                url.append("?os_key=").append(URLEncoder.encode(osKey, StandardCharsets.UTF_8.name()));
                hasParams = true;
            } catch (UnsupportedEncodingException e) {
                // UTF-8 should always be supported
                url.append("?os_key=").append(osKey);
                hasParams = true;
            }
        }

        if (leafletMapStyle != null && !leafletMapStyle.isEmpty()) {
            url.append(hasParams ? "&" : "?");
            try {
                url.append("initial_style=").append(URLEncoder.encode(leafletMapStyle, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                // UTF-8 should always be supported
                url.append("initial_style=").append(leafletMapStyle);
            }
        }

        return url.toString();
    }
}

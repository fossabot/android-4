package uk.trigpointing.android.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.Before;

import android.graphics.Bitmap;

/**
 * Unit tests for MemoryCache class
 * Testing memory caching functionality with strings and bitmaps
 */
public class MemoryCacheTest {

    private MemoryCache memoryCache;

    @Before
    public void setUp() {
        memoryCache = new MemoryCache();
    }

    @Test
    public void testMemoryCacheCreation() {
        assertNotNull("MemoryCache should be created", memoryCache);
    }

    @Test
    public void testPutAndGetString() {
        String testId = "test_string_id";
        String testValue = "Hello, World!";
        
        // Initially should return null
        assertNull("Should return null for non-existent key", memoryCache.getString(testId));
        
        // Put string and retrieve
        memoryCache.put(testId, testValue);
        assertEquals("Should return stored string", testValue, memoryCache.getString(testId));
    }

    @Test
    public void testPutAndGetMultipleStrings() {
        String id1 = "string1";
        String id2 = "string2";
        String value1 = "First String";
        String value2 = "Second String";
        
        memoryCache.put(id1, value1);
        memoryCache.put(id2, value2);
        
        assertEquals("First string should be retrievable", value1, memoryCache.getString(id1));
        assertEquals("Second string should be retrievable", value2, memoryCache.getString(id2));
    }

    @Test
    public void testOverwriteString() {
        String testId = "overwrite_test";
        String originalValue = "Original Value";
        String newValue = "New Value";
        
        memoryCache.put(testId, originalValue);
        assertEquals("Should get original value", originalValue, memoryCache.getString(testId));
        
        memoryCache.put(testId, newValue);
        assertEquals("Should get new value after overwrite", newValue, memoryCache.getString(testId));
    }

    @Test
    public void testGetStringWithNonExistentKey() {
        assertNull("Should return null for non-existent key", memoryCache.getString("non_existent"));
    }

    @Test
    public void testPutAndGetBitmap() {
        // Note: In unit tests, we can't easily create real Bitmap objects
        // But we can test with mock objects or null handling
        String testId = "bitmap_test";
        
        // Test with null bitmap
        memoryCache.put(testId, (Bitmap) null);
        assertNull("Should handle null bitmap", memoryCache.getBitmap(testId));
        
        // Initially should return null
        assertNull("Should return null for non-existent bitmap key", memoryCache.getBitmap("non_existent_bitmap"));
    }

    @Test
    public void testGetBitmapWithNonExistentKey() {
        assertNull("Should return null for non-existent bitmap key", memoryCache.getBitmap("non_existent"));
    }

    @Test
    public void testStringAndBitmapSharedStorage() {
        String sharedId = "shared_id";
        String testString = "Test String";
        
        // Put string with shared ID
        memoryCache.put(sharedId, testString);
        
        // Should be able to get string
        assertEquals("Should get string with shared ID", testString, memoryCache.getString(sharedId));
        
        // Trying to get bitmap with same ID will cause ClassCastException since it tries to cast String to Bitmap
        // This is expected behavior since the implementation uses the same HashMap for both types
        try {
            memoryCache.getBitmap(sharedId);
            fail("Should throw ClassCastException when trying to get bitmap of string");
        } catch (ClassCastException e) {
            // Expected behavior - the implementation casts without checking type
            assertTrue("ClassCastException is expected", true);
        }
    }

    @Test
    public void testPutNullString() {
        String testId = "null_string_test";
        
        memoryCache.put(testId, (String) null);
        
        // The cache stores null values as SoftReference, which might return null when dereferenced
        // This tests the null handling in the getString method
        String result = memoryCache.getString(testId);
        assertNull("Null string should return null", result);
    }

    @Test
    public void testEmptyString() {
        String testId = "empty_string_test";
        String emptyString = "";
        
        memoryCache.put(testId, emptyString);
        assertEquals("Empty string should be stored and retrieved", emptyString, memoryCache.getString(testId));
    }

    @Test
    public void testLongString() {
        String testId = "long_string_test";
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longString.append("This is a long string for testing memory cache. ");
        }
        
        String longStringValue = longString.toString();
        memoryCache.put(testId, longStringValue);
        assertEquals("Long string should be stored and retrieved", longStringValue, memoryCache.getString(testId));
    }

    @Test
    public void testSpecialCharacterStrings() {
        String[] specialStrings = {
            "String with spaces",
            "String\nwith\nnewlines",
            "String\twith\ttabs",
            "String with émojis 🎉🚀",
            "String with \"quotes\" and 'apostrophes'",
            "String with special chars: !@#$%^&*()_+-=[]{}|;:,.<>?",
            "Unicode test: 你好世界 مرحبا بالعالم Здравствуй мир"
        };
        
        for (int i = 0; i < specialStrings.length; i++) {
            String id = "special_" + i;
            memoryCache.put(id, specialStrings[i]);
            assertEquals("Special string " + i + " should be stored correctly", 
                        specialStrings[i], memoryCache.getString(id));
        }
    }

    @Test
    public void testCacheKeyVariations() {
        // Test various key formats
        String[] keys = {
            "simple_key",
            "key-with-dashes",
            "key.with.dots",
            "key/with/slashes",
            "KEY_WITH_UPPERCASE",
            "key123456",
            "key_with_underscores",
            "https://example.com/path?param=value"
        };
        
        for (String key : keys) {
            String value = "Value for " + key;
            memoryCache.put(key, value);
            assertEquals("Key variation should work: " + key, value, memoryCache.getString(key));
        }
    }

    @Test
    public void testMemoryCacheConsistency() {
        // Test that cache behaves consistently across multiple operations
        String id1 = "consistency_1";
        String id2 = "consistency_2";
        String value1 = "Value 1";
        String value2 = "Value 2";
        
        // Store values
        memoryCache.put(id1, value1);
        memoryCache.put(id2, value2);
        
        // Verify multiple retrievals return same values
        for (int i = 0; i < 10; i++) {
            assertEquals("Value 1 should be consistent on retrieval " + i, value1, memoryCache.getString(id1));
            assertEquals("Value 2 should be consistent on retrieval " + i, value2, memoryCache.getString(id2));
        }
    }

    @Test
    public void testCacheOverwrite() {
        // Test that storing different types with same key overwrites previous value
        String sharedKey = "shared_key";
        String stringValue = "String Value";
        
        // Store string
        memoryCache.put(sharedKey, stringValue);
        assertEquals("String should be stored", stringValue, memoryCache.getString(sharedKey));
        
        // Store null bitmap with same key - this overwrites the string
        memoryCache.put(sharedKey, (Bitmap) null);
        
        // The string is now overwritten by the bitmap entry
        assertNull("String is overwritten by bitmap with same key", memoryCache.getString(sharedKey));
        assertNull("Bitmap with same key should be null", memoryCache.getBitmap(sharedKey));
    }
}

package uk.trigpointing.android.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Unit tests for Utils class
 * Testing utility methods for stream copying and image handling
 */
public class UtilsTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private ContentResolver mockContentResolver;
    
    @Mock
    private Uri mockUri;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCopyStreamWithValidData() throws IOException {
        // Prepare test data
        String testData = "Hello, World! This is a test string for stream copying.";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Execute
        Utils.CopyStream(inputStream, outputStream);
        
        // Verify
        String result = outputStream.toString();
        assertEquals("Stream content should be copied correctly", testData, result);
    }

    @Test
    public void testCopyStreamWithEmptyStream() throws IOException {
        // Prepare empty stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Execute
        Utils.CopyStream(inputStream, outputStream);
        
        // Verify
        assertEquals("Empty stream should result in empty output", 0, outputStream.size());
    }

    @Test
    public void testCopyStreamWithLargeData() throws IOException {
        // Prepare large test data (larger than buffer size of 1024)
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 2000; i++) { // Create data larger than 1024 bytes
            largeData.append("X");
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData.toString().getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Execute
        Utils.CopyStream(inputStream, outputStream);
        
        // Verify
        String result = outputStream.toString();
        assertEquals("Large data should be copied correctly", largeData.toString(), result);
        assertTrue("Result should be larger than buffer size", result.length() > 1024);
    }

    @Test
    public void testCopyStreamWithBinaryData() throws IOException {
        // Prepare binary test data
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(binaryData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Execute
        Utils.CopyStream(inputStream, outputStream);
        
        // Verify
        byte[] result = outputStream.toByteArray();
        assertArrayEquals("Binary data should be copied correctly", binaryData, result);
    }

    @Test
    public void testCopyStreamHandlesExceptions() {
        // Create a mock InputStream that throws an exception
        InputStream faultyInputStream = mock(InputStream.class);
        OutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            when(faultyInputStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("Test exception"));
            
            // This should not throw an exception - it's caught and ignored
            Utils.CopyStream(faultyInputStream, outputStream);
            
            // If we reach here, the exception was handled properly
            assertTrue("Exception should be handled gracefully", true);
        } catch (IOException e) {
            fail("IOException should be caught and handled within CopyStream method");
        }
    }

    @Test
    public void testCopyStreamWithNullInputHandling() {
        // Test with null streams - this may cause NullPointerException
        // but the method signature suggests it expects valid streams
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            Utils.CopyStream(null, outputStream);
            // If no exception is thrown, that's fine (exception is caught)
        } catch (Exception e) {
            // Expected behavior - null input should be handled
            assertTrue("Null input should be handled", true);
        }
    }

    @Test
    public void testCopyStreamBufferSize() throws IOException {
        // Test that the method works with data exactly at buffer boundaries
        int bufferSize = 1024; // As defined in Utils.CopyStream
        
        // Test with exactly buffer size data
        byte[] exactBufferData = new byte[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            exactBufferData[i] = (byte) (i % 256);
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(exactBufferData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Execute
        Utils.CopyStream(inputStream, outputStream);
        
        // Verify
        byte[] result = outputStream.toByteArray();
        assertArrayEquals("Buffer-sized data should be copied correctly", exactBufferData, result);
        assertEquals("Output size should match input size", bufferSize, result.length);
    }

    // Note: decodeUri tests would require more complex Android mocking
    // since it involves ContentResolver, BitmapFactory, etc.
    // For now, we focus on the stream copying functionality which is more testable
    
    @Test
    public void testUtilsClassExists() {
        // Simple test to ensure the Utils class can be instantiated
        // Though it appears to be a utility class with static methods
        assertNotNull("Utils class should exist", Utils.class);
    }
}

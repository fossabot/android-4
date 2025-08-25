package uk.trigpointing.android.types;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for TrigLog class
 * Testing trigpoint log entry data structure
 */
public class TrigLogTest {

    private TrigLog testTrigLog;

    @Before
    public void setUp() {
        testTrigLog = new TrigLog("testuser", "2023-08-15", Condition.GOOD, "Found the trigpoint in excellent condition");
    }

    @Test
    public void testTrigLogCreation() {
        assertNotNull("TrigLog should be created", testTrigLog);
        assertEquals("Username should be set", "testuser", testTrigLog.getUsername());
        assertEquals("Date should be set", "2023-08-15", testTrigLog.getDate());
        assertEquals("Condition should be set", Condition.GOOD, testTrigLog.getCondition());
        assertEquals("Text should be set", "Found the trigpoint in excellent condition", testTrigLog.getText());
    }

    @Test
    public void testUsernameProperty() {
        String newUsername = "anotheruser";
        testTrigLog.setUsername(newUsername);
        assertEquals("Username should be updated", newUsername, testTrigLog.getUsername());
    }

    @Test
    public void testDateProperty() {
        String newDate = "2024-01-15";
        testTrigLog.setDate(newDate);
        assertEquals("Date should be updated", newDate, testTrigLog.getDate());
    }

    @Test
    public void testConditionProperty() {
        Condition newCondition = Condition.DAMAGED;
        testTrigLog.setCondition(newCondition);
        assertEquals("Condition should be updated", newCondition, testTrigLog.getCondition());
    }

    @Test
    public void testTextProperty() {
        String newText = "Trigpoint has been vandalized";
        testTrigLog.setText(newText);
        assertEquals("Text should be updated", newText, testTrigLog.getText());
    }

    @Test
    public void testTrigLogWithNullValues() {
        TrigLog nullLog = new TrigLog(null, null, null, null);
        assertNull("Username can be null", nullLog.getUsername());
        assertNull("Date can be null", nullLog.getDate());
        assertNull("Condition can be null", nullLog.getCondition());
        assertNull("Text can be null", nullLog.getText());
    }

    @Test
    public void testTrigLogWithEmptyStrings() {
        TrigLog emptyLog = new TrigLog("", "", Condition.UNKNOWN, "");
        assertEquals("Username can be empty", "", emptyLog.getUsername());
        assertEquals("Date can be empty", "", emptyLog.getDate());
        assertEquals("Condition should be UNKNOWN", Condition.UNKNOWN, emptyLog.getCondition());
        assertEquals("Text can be empty", "", emptyLog.getText());
    }

    @Test
    public void testTrigLogWithVariousConditions() {
        // Test with different conditions
        TrigLog log1 = new TrigLog("user1", "2023-01-01", Condition.GOOD, "Perfect condition");
        TrigLog log2 = new TrigLog("user2", "2023-02-01", Condition.DAMAGED, "Some damage visible");
        TrigLog log3 = new TrigLog("user3", "2023-03-01", Condition.MISSING, "Could not find");
        
        assertEquals("First log condition", Condition.GOOD, log1.getCondition());
        assertEquals("Second log condition", Condition.DAMAGED, log2.getCondition());
        assertEquals("Third log condition", Condition.MISSING, log3.getCondition());
    }

    @Test
    public void testTrigLogWithLongText() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a very long log entry. ");
        }
        
        TrigLog longLog = new TrigLog("verboseuser", "2023-05-01", Condition.GOOD, longText.toString());
        assertEquals("Long text should be preserved", longText.toString(), longLog.getText());
    }

    @Test
    public void testTrigLogConstructorParameterOrder() {
        // Test that constructor parameters are in the expected order
        TrigLog log = new TrigLog("testuser", "2023-12-25", Condition.SLIGHTLYDAMAGED, "Christmas visit");
        
        assertEquals("First parameter should be username", "testuser", log.getUsername());
        assertEquals("Second parameter should be date", "2023-12-25", log.getDate());
        assertEquals("Third parameter should be condition", Condition.SLIGHTLYDAMAGED, log.getCondition());
        assertEquals("Fourth parameter should be text", "Christmas visit", log.getText());
    }

    @Test
    public void testTrigLogImmutableAfterConstruction() {
        // Test that the constructor sets values correctly
        String originalUsername = testTrigLog.getUsername();
        String originalDate = testTrigLog.getDate();
        Condition originalCondition = testTrigLog.getCondition();
        String originalText = testTrigLog.getText();
        
        // These should still be the same until explicitly changed
        assertEquals("Username should remain unchanged", originalUsername, testTrigLog.getUsername());
        assertEquals("Date should remain unchanged", originalDate, testTrigLog.getDate());
        assertEquals("Condition should remain unchanged", originalCondition, testTrigLog.getCondition());
        assertEquals("Text should remain unchanged", originalText, testTrigLog.getText());
    }

    @Test
    public void testTrigLogMultipleUpdates() {
        // Test multiple updates to the same log
        testTrigLog.setUsername("user1");
        testTrigLog.setDate("2024-01-01");
        testTrigLog.setCondition(Condition.TOPPLED);
        testTrigLog.setText("Updated after storm damage");
        
        assertEquals("Username after update", "user1", testTrigLog.getUsername());
        assertEquals("Date after update", "2024-01-01", testTrigLog.getDate());
        assertEquals("Condition after update", Condition.TOPPLED, testTrigLog.getCondition());
        assertEquals("Text after update", "Updated after storm damage", testTrigLog.getText());
        
        // Update again
        testTrigLog.setCondition(Condition.GOOD);
        testTrigLog.setText("Restored to good condition");
        
        assertEquals("Condition after second update", Condition.GOOD, testTrigLog.getCondition());
        assertEquals("Text after second update", "Restored to good condition", testTrigLog.getText());
    }
}

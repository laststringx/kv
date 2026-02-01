package com.kvds.wal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogEntry Tests")
class LogEntryTest {
    
    @Test
    @DisplayName("Should create log entry with timestamp")
    void testCreateLogEntryWithTimestamp() {
        long timestamp = System.currentTimeMillis();
        LogEntry entry = new LogEntry(timestamp, Operation.PUT, "key1", "value1");
        
        assertEquals(timestamp, entry.getTimestamp());
        assertEquals(Operation.PUT, entry.getOperation());
        assertEquals("key1", entry.getKey());
        assertEquals("value1", entry.getValue());
    }
    
    @Test
    @DisplayName("Should create log entry without timestamp")
    void testCreateLogEntryWithoutTimestamp() {
        LogEntry entry = new LogEntry(Operation.PUT, "key1", "value1");
        
        assertTrue(entry.getTimestamp() > 0);
        assertEquals(Operation.PUT, entry.getOperation());
        assertEquals("key1", entry.getKey());
        assertEquals("value1", entry.getValue());
    }
    
    @Test
    @DisplayName("Should create DELETE entry with null value")
    void testCreateDeleteEntry() {
        LogEntry entry = new LogEntry(Operation.DELETE, "key1", null);
        
        assertEquals(Operation.DELETE, entry.getOperation());
        assertEquals("key1", entry.getKey());
        assertNull(entry.getValue());
    }
    
    @Test
    @DisplayName("Should throw exception for null operation")
    void testNullOperation() {
        assertThrows(NullPointerException.class, 
            () -> new LogEntry(null, "key1", "value1"));
    }
    
    @Test
    @DisplayName("Should throw exception for null key")
    void testNullKey() {
        assertThrows(NullPointerException.class, 
            () -> new LogEntry(Operation.PUT, null, "value1"));
    }
    
    @Test
    @DisplayName("Should serialize PUT entry")
    void testSerializePutEntry() {
        LogEntry entry = new LogEntry(1738419540000L, Operation.PUT, "key1", "value1");
        String serialized = entry.serialize();
        
        assertEquals("1738419540000|PUT|key1|value1", serialized);
    }
    
    @Test
    @DisplayName("Should serialize DELETE entry")
    void testSerializeDeleteEntry() {
        LogEntry entry = new LogEntry(1738419540000L, Operation.DELETE, "key1", null);
        String serialized = entry.serialize();
        
        assertEquals("1738419540000|DELETE|key1|null", serialized);
    }
    
    @Test
    @DisplayName("Should deserialize PUT entry")
    void testDeserializePutEntry() {
        String line = "1738419540000|PUT|key1|value1";
        LogEntry entry = LogEntry.deserialize(line);
        
        assertEquals(1738419540000L, entry.getTimestamp());
        assertEquals(Operation.PUT, entry.getOperation());
        assertEquals("key1", entry.getKey());
        assertEquals("value1", entry.getValue());
    }
    
    @Test
    @DisplayName("Should deserialize DELETE entry")
    void testDeserializeDeleteEntry() {
        String line = "1738419540000|DELETE|key1|null";
        LogEntry entry = LogEntry.deserialize(line);
        
        assertEquals(1738419540000L, entry.getTimestamp());
        assertEquals(Operation.DELETE, entry.getOperation());
        assertEquals("key1", entry.getKey());
        assertNull(entry.getValue());
    }
    
    @Test
    @DisplayName("Should handle value with spaces")
    void testSerializeDeserializeWithSpaces() {
        LogEntry original = new LogEntry(1738419540000L, Operation.PUT, "key1", "value with spaces");
        String serialized = original.serialize();
        LogEntry deserialized = LogEntry.deserialize(serialized);
        
        assertEquals(original, deserialized);
    }
    
    @Test
    @DisplayName("Should throw exception for null line on deserialize")
    void testDeserializeNullLine() {
        assertThrows(IllegalArgumentException.class, () -> LogEntry.deserialize(null));
    }
    
    @Test
    @DisplayName("Should throw exception for empty line on deserialize")
    void testDeserializeEmptyLine() {
        assertThrows(IllegalArgumentException.class, () -> LogEntry.deserialize(""));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid format")
    void testDeserializeInvalidFormat() {
        assertThrows(IllegalArgumentException.class, 
            () -> LogEntry.deserialize("invalid|format"));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid timestamp")
    void testDeserializeInvalidTimestamp() {
        assertThrows(IllegalArgumentException.class, 
            () -> LogEntry.deserialize("notanumber|PUT|key1|value1"));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid operation")
    void testDeserializeInvalidOperation() {
        assertThrows(IllegalArgumentException.class, 
            () -> LogEntry.deserialize("1738419540000|INVALID|key1|value1"));
    }
    
    @Test
    @DisplayName("Should implement equals correctly")
    void testEquals() {
        LogEntry entry1 = new LogEntry(1738419540000L, Operation.PUT, "key1", "value1");
        LogEntry entry2 = new LogEntry(1738419540000L, Operation.PUT, "key1", "value1");
        LogEntry entry3 = new LogEntry(1738419540000L, Operation.PUT, "key2", "value2");
        
        assertEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
    }
    
    @Test
    @DisplayName("Should implement hashCode correctly")
    void testHashCode() {
        LogEntry entry1 = new LogEntry(1738419540000L, Operation.PUT, "key1", "value1");
        LogEntry entry2 = new LogEntry(1738419540000L, Operation.PUT, "key1", "value1");
        
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }
    
    @Test
    @DisplayName("Should implement toString")
    void testToString() {
        LogEntry entry = new LogEntry(1738419540000L, Operation.PUT, "key1", "value1");
        String str = entry.toString();
        
        assertTrue(str.contains("PUT"));
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("value1"));
    }
}

package com.kvds.wal;

import java.util.Objects;

/**
 * Represents a single entry in the Write-Ahead Log.
 * 
 * Format: timestamp|operation|key|value
 * Example: 1738419540000|PUT|user:1|John Doe
 */
public class LogEntry {
    
    private static final String DELIMITER = "|";
    private static final String NULL_VALUE = "null";
    
    private final long timestamp;
    private final Operation operation;
    private final String key;
    private final String value;
    
    /**
     * Creates a new LogEntry.
     * 
     * @param timestamp the timestamp in milliseconds
     * @param operation the operation type
     * @param key the key
     * @param value the value (can be null for DELETE operations)
     */
    public LogEntry(long timestamp, Operation operation, String key, String value) {
        this.timestamp = timestamp;
        this.operation = Objects.requireNonNull(operation, "Operation cannot be null");
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.value = value;
    }
    
    /**
     * Creates a new LogEntry with current timestamp.
     * 
     * @param operation the operation type
     * @param key the key
     * @param value the value (can be null for DELETE operations)
     */
    public LogEntry(Operation operation, String key, String value) {
        this(System.currentTimeMillis(), operation, key, value);
    }
    
    /**
     * Serializes this log entry to a string.
     * 
     * @return the serialized log entry
     */
    public String serialize() {
        String valueStr = (value == null) ? NULL_VALUE : value;
        return timestamp + DELIMITER + operation + DELIMITER + key + DELIMITER + valueStr;
    }
    
    /**
     * Deserializes a log entry from a string.
     * 
     * @param line the serialized log entry
     * @return the deserialized LogEntry
     * @throws IllegalArgumentException if the line format is invalid
     */
    public static LogEntry deserialize(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Log entry line cannot be null or empty");
        }
        
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid log entry format: " + line);
        }
        
        try {
            long timestamp = Long.parseLong(parts[0]);
            Operation operation = Operation.valueOf(parts[1]);
            String key = parts[2];
            String value = NULL_VALUE.equals(parts[3]) ? null : parts[3];
            
            return new LogEntry(timestamp, operation, key, value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid timestamp in log entry: " + line, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation in log entry: " + line, e);
        }
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return timestamp == logEntry.timestamp &&
                operation == logEntry.operation &&
                Objects.equals(key, logEntry.key) &&
                Objects.equals(value, logEntry.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, operation, key, value);
    }
    
    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", operation=" + operation +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

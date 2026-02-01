package com.kvds.wal;

import java.util.List;

/**
 * WriteAheadLog interface for durable logging of operations.
 * 
 * This interface follows the Interface Segregation Principle (ISP)
 * by providing only WAL-related operations.
 */
public interface WriteAheadLog {
    
    /**
     * Appends a log entry to the WAL.
     * This operation must be durable (fsync).
     * 
     * @param entry the log entry to append
     * @throws com.kvds.exception.KVDSException if the append fails
     */
    void append(LogEntry entry);
    
    /**
     * Reads all log entries from the WAL.
     * Used during recovery.
     * 
     * @return list of all log entries
     * @throws com.kvds.exception.KVDSException if reading fails
     */
    List<LogEntry> readAll();
    
    /**
     * Clears all entries from the WAL.
     * This operation is NOT typically used in production.
     * 
     * @throws com.kvds.exception.KVDSException if clearing fails
     */
    void clear();
    
    /**
     * Closes the WAL and releases all resources.
     * 
     * @throws com.kvds.exception.KVDSException if closing fails
     */
    void close();
}

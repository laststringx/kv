package com.kvds.recovery;

import com.kvds.exception.KVDSException;
import com.kvds.storage.Storage;
import com.kvds.wal.LogEntry;
import com.kvds.wal.Operation;
import com.kvds.wal.WriteAheadLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of RecoveryManager.
 * 
 * This class follows the Single Responsibility Principle (SRP)
 * by focusing solely on recovery logic.
 */
public class RecoveryManagerImpl implements RecoveryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RecoveryManagerImpl.class);
    
    @Override
    public void recover(WriteAheadLog wal, Storage storage) {
        if (wal == null) {
            throw new IllegalArgumentException("WAL cannot be null");
        }
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        
        try {
            logger.info("Starting recovery from WAL");
            
            List<LogEntry> entries = wal.readAll();
            
            if (entries.isEmpty()) {
                logger.info("No entries to recover");
                return;
            }
            
            int recoveredCount = 0;
            for (LogEntry entry : entries) {
                try {
                    replayLogEntry(entry, storage);
                    recoveredCount++;
                } catch (Exception e) {
                    // Log error but continue with next entry
                    logger.warn("Failed to replay log entry: {}", entry, e);
                }
            }
            
            logger.info("Recovery complete: {} entries replayed out of {}", 
                recoveredCount, entries.size());
            
        } catch (Exception e) {
            logger.error("Recovery failed", e);
            throw new KVDSException("Recovery failed", e);
        }
    }
    
    /**
     * Replays a single log entry to storage.
     * 
     * @param entry the log entry to replay
     * @param storage the storage to update
     */
    private void replayLogEntry(LogEntry entry, Storage storage) {
        if (entry.getOperation() == Operation.PUT) {
            storage.put(entry.getKey(), entry.getValue());
            logger.trace("Replayed PUT: key={}", entry.getKey());
        } else if (entry.getOperation() == Operation.DELETE) {
            storage.delete(entry.getKey());
            logger.trace("Replayed DELETE: key={}", entry.getKey());
        } else {
            logger.warn("Unknown operation type: {}", entry.getOperation());
        }
    }
}

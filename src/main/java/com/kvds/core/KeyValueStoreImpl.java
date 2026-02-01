package com.kvds.core;

import com.kvds.exception.KVDSException;
import com.kvds.recovery.RecoveryManager;
import com.kvds.recovery.RecoveryManagerImpl;
import com.kvds.storage.Storage;
import com.kvds.wal.LogEntry;
import com.kvds.wal.Operation;
import com.kvds.wal.WriteAheadLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of KeyValueStore with Write-Ahead Logging and Recovery.
 * 
 * This class follows the Single Responsibility Principle (SRP)
 * by coordinating operations between Storage, WAL, and Recovery.
 * 
 * It follows the Dependency Inversion Principle (DIP) by depending
 * on the Storage and WriteAheadLog interfaces rather than concrete implementations.
 * 
 * Write operations follow the WAL-first pattern:
 * 1. Write to WAL (durable)
 * 2. Update in-memory storage
 * 
 * On startup with WAL enabled, the store automatically recovers from the WAL.
 */
public class KeyValueStoreImpl implements KeyValueStore {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyValueStoreImpl.class);
    
    private final Storage storage;
    private final WriteAheadLog wal;
    private final RecoveryManager recoveryManager;
    
    /**
     * Creates a new KeyValueStore with the given storage and WAL.
     * Automatically performs recovery from WAL on startup.
     * 
     * @param storage the storage implementation
     * @param wal the write-ahead log implementation
     */
    public KeyValueStoreImpl(Storage storage, WriteAheadLog wal) {
        this(storage, wal, new RecoveryManagerImpl());
    }
    
    /**
     * Creates a new KeyValueStore with the given storage, WAL, and recovery manager.
     * This constructor allows for dependency injection of RecoveryManager (useful for testing).
     * Automatically performs recovery from WAL on startup.
     * 
     * @param storage the storage implementation
     * @param wal the write-ahead log implementation
     * @param recoveryManager the recovery manager implementation
     */
    public KeyValueStoreImpl(Storage storage, WriteAheadLog wal, RecoveryManager recoveryManager) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        if (wal == null) {
            throw new IllegalArgumentException("WAL cannot be null");
        }
        if (recoveryManager == null) {
            throw new IllegalArgumentException("RecoveryManager cannot be null");
        }
        
        this.storage = storage;
        this.wal = wal;
        this.recoveryManager = recoveryManager;
        
        // Perform recovery from WAL on startup
        try {
            recoveryManager.recover(wal, storage);
            logger.info("KeyValueStore initialized with WAL and recovery complete");
        } catch (Exception e) {
            logger.error("Failed to recover from WAL", e);
            throw new KVDSException("Failed to initialize KeyValueStore: recovery failed", e);
        }
    }
    
    /**
     * Creates a new KeyValueStore with the given storage (without WAL).
     * This constructor is for backward compatibility and testing.
     * 
     * @param storage the storage implementation
     */
    public KeyValueStoreImpl(Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Storage cannot be null");
        }
        this.storage = storage;
        this.wal = null;
        this.recoveryManager = null;
        logger.info("KeyValueStore initialized without WAL");
    }
    
    @Override
    public void put(String key, String value) {
        validateKey(key);
        validateValue(value);
        
        try {
            // WAL-first pattern: Write to WAL before updating storage
            if (wal != null) {
                LogEntry entry = new LogEntry(Operation.PUT, key, value);
                wal.append(entry);
            }
            
            // Update in-memory storage
            storage.put(key, value);
            logger.debug("PUT successful: key={}", key);
        } catch (Exception e) {
            logger.error("PUT failed: key={}", key, e);
            throw new KVDSException("Failed to put key: " + key, e);
        }
    }
    
    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        try {
            String value = storage.get(key);
            logger.debug("GET: key={}, found={}", key, value != null);
            return value;
        } catch (Exception e) {
            logger.error("GET failed: key={}", key, e);
            throw new KVDSException("Failed to get key: " + key, e);
        }
    }
    
    @Override
    public void delete(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        
        try {
            // WAL-first pattern: Write to WAL before updating storage
            if (wal != null) {
                LogEntry entry = new LogEntry(Operation.DELETE, key, null);
                wal.append(entry);
            }
            
            // Update in-memory storage
            storage.delete(key);
            logger.debug("DELETE successful: key={}", key);
        } catch (Exception e) {
            logger.error("DELETE failed: key={}", key, e);
            throw new KVDSException("Failed to delete key: " + key, e);
        }
    }
    
    @Override
    public void clear() {
        try {
            storage.clear();
            logger.info("Store cleared");
        } catch (Exception e) {
            logger.error("CLEAR failed", e);
            throw new KVDSException("Failed to clear store", e);
        }
    }
    
    @Override
    public void close() {
        try {
            if (wal != null) {
                wal.close();
            }
            logger.info("KeyValueStore closed");
        } catch (Exception e) {
            logger.error("CLOSE failed", e);
            throw new KVDSException("Failed to close store", e);
        }
    }
    
    private void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        // Validate that key doesn't contain pipe character (used as delimiter in WAL)
        if (key.contains("|")) {
            throw new IllegalArgumentException("Key cannot contain pipe character '|'");
        }
    }
    
    private void validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        // Validate that value doesn't contain pipe character (used as delimiter in WAL)
        if (value.contains("|")) {
            throw new IllegalArgumentException("Value cannot contain pipe character '|'");
        }
    }
}

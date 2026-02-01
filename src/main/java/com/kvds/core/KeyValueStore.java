package com.kvds.core;

/**
 * KeyValueStore interface for basic key-value operations.
 * 
 * This interface follows the Interface Segregation Principle (ISP)
 * by providing only core KV operations.
 */
public interface KeyValueStore {
    
    /**
     * Stores a key-value pair.
     * The operation is durable - it will survive crashes.
     * 
     * @param key the key (must not be null or empty)
     * @param value the value (must not be null)
     * @throws com.kvds.exception.KVDSException if the operation fails
     */
    void put(String key, String value);
    
    /**
     * Retrieves the value associated with the given key.
     * 
     * @param key the key to lookup
     * @return the value associated with the key, or null if not found
     */
    String get(String key);
    
    /**
     * Removes the key-value pair associated with the given key.
     * The operation is durable - it will survive crashes.
     * 
     * @param key the key to remove
     * @throws com.kvds.exception.KVDSException if the operation fails
     */
    void delete(String key);
    
    /**
     * Removes all key-value pairs from the store.
     * This operation is NOT durable and is intended for testing.
     */
    void clear();
    
    /**
     * Closes the store and releases all resources.
     * After calling this method, the store should not be used.
     * 
     * @throws com.kvds.exception.KVDSException if closing fails
     */
    void close();
}

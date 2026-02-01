package com.kvds.storage;

/**
 * Storage interface for key-value operations.
 * Implementations must be thread-safe.
 * 
 * This interface follows the Interface Segregation Principle (ISP)
 * by providing only storage-related operations.
 */
public interface Storage {
    
    /**
     * Stores a key-value pair.
     * If the key already exists, the value is updated.
     * 
     * @param key the key (must not be null)
     * @param value the value (must not be null)
     * @throws IllegalArgumentException if key or value is null
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
     * 
     * @param key the key to remove
     */
    void delete(String key);
    
    /**
     * Removes all key-value pairs from storage.
     */
    void clear();
    
    /**
     * Checks if the given key exists in storage.
     * 
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(String key);
    
    /**
     * Returns the number of key-value pairs in storage.
     * 
     * @return the size of the storage
     */
    int size();
}

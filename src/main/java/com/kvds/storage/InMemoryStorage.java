package com.kvds.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory storage implementation using ConcurrentHashMap.
 * 
 * This class follows the Single Responsibility Principle (SRP)
 * by focusing solely on managing in-memory data storage.
 */
public class InMemoryStorage implements Storage {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryStorage.class);
    
    private final ConcurrentHashMap<String, String> store;
    
    /**
     * Creates a new InMemoryStorage instance.
     */
    public InMemoryStorage() {
        this.store = new ConcurrentHashMap<>();
        logger.debug("InMemoryStorage initialized");
    }
    
    @Override
    public void put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        store.put(key, value);
        logger.trace("PUT: key={}, value={}", key, value);
    }
    
    @Override
    public String get(String key) {
        if (key == null) {
            return null;
        }
        
        String value = store.get(key);
        logger.trace("GET: key={}, value={}", key, value);
        return value;
    }
    
    @Override
    public void delete(String key) {
        if (key == null) {
            return;
        }
        
        store.remove(key);
        logger.trace("DELETE: key={}", key);
    }
    
    @Override
    public void clear() {
        store.clear();
        logger.debug("Storage cleared");
    }
    
    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        return store.containsKey(key);
    }
    
    @Override
    public int size() {
        return store.size();
    }
}

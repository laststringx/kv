package com.kvds.core;

import com.kvds.exception.KVDSException;
import com.kvds.storage.InMemoryStorage;
import com.kvds.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KeyValueStoreImpl Tests")
class KeyValueStoreImplTest {
    
    private KeyValueStore store;
    private Storage storage;
    
    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        store = new KeyValueStoreImpl(storage);
    }
    
    @Test
    @DisplayName("Should create store with valid storage")
    void testCreateStore() {
        assertNotNull(store);
    }
    
    @Test
    @DisplayName("Should throw exception when storage is null")
    void testCreateStoreWithNullStorage() {
        assertThrows(IllegalArgumentException.class, () -> new KeyValueStoreImpl(null));
    }
    
    @Test
    @DisplayName("Should store and retrieve value")
    void testPutAndGet() {
        store.put("key1", "value1");
        assertEquals("value1", store.get("key1"));
    }
    
    @Test
    @DisplayName("Should return null for non-existent key")
    void testGetNonExistentKey() {
        assertNull(store.get("nonexistent"));
    }
    
    @Test
    @DisplayName("Should update existing key")
    void testUpdateExistingKey() {
        store.put("key1", "value1");
        store.put("key1", "value2");
        assertEquals("value2", store.get("key1"));
    }
    
    @Test
    @DisplayName("Should delete key")
    void testDelete() {
        store.put("key1", "value1");
        store.delete("key1");
        assertNull(store.get("key1"));
    }
    
    @Test
    @DisplayName("Should handle delete on non-existent key")
    void testDeleteNonExistentKey() {
        assertDoesNotThrow(() -> store.delete("nonexistent"));
    }
    
    @Test
    @DisplayName("Should clear all entries")
    void testClear() {
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.clear();
        assertNull(store.get("key1"));
        assertNull(store.get("key2"));
    }
    
    @Test
    @DisplayName("Should throw exception for null key on put")
    void testPutNullKey() {
        assertThrows(IllegalArgumentException.class, () -> store.put(null, "value"));
    }
    
    @Test
    @DisplayName("Should throw exception for empty key on put")
    void testPutEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> store.put("", "value"));
    }
    
    @Test
    @DisplayName("Should throw exception for null value on put")
    void testPutNullValue() {
        assertThrows(IllegalArgumentException.class, () -> store.put("key", null));
    }
    
    @Test
    @DisplayName("Should throw exception for key containing pipe character")
    void testPutKeyWithPipe() {
        assertThrows(IllegalArgumentException.class, () -> store.put("key|with|pipe", "value"));
    }
    
    @Test
    @DisplayName("Should throw exception for value containing pipe character")
    void testPutValueWithPipe() {
        assertThrows(IllegalArgumentException.class, () -> store.put("key", "value|with|pipe"));
    }
    
    @Test
    @DisplayName("Should handle null key on get")
    void testGetNullKey() {
        assertNull(store.get(null));
    }
    
    @Test
    @DisplayName("Should handle empty key on get")
    void testGetEmptyKey() {
        assertNull(store.get(""));
    }
    
    @Test
    @DisplayName("Should handle null key on delete")
    void testDeleteNullKey() {
        assertDoesNotThrow(() -> store.delete(null));
    }
    
    @Test
    @DisplayName("Should handle empty key on delete")
    void testDeleteEmptyKey() {
        assertDoesNotThrow(() -> store.delete(""));
    }
    
    @Test
    @DisplayName("Should close without errors")
    void testClose() {
        assertDoesNotThrow(() -> store.close());
    }
    
    @Test
    @DisplayName("Should handle multiple operations")
    void testMultipleOperations() {
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.put("key3", "value3");
        
        assertEquals("value1", store.get("key1"));
        assertEquals("value2", store.get("key2"));
        assertEquals("value3", store.get("key3"));
        
        store.delete("key2");
        assertNull(store.get("key2"));
        
        assertEquals("value1", store.get("key1"));
        assertEquals("value3", store.get("key3"));
    }
}

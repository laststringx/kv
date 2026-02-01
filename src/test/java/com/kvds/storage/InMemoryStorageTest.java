package com.kvds.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryStorage Tests")
class InMemoryStorageTest {
    
    private InMemoryStorage storage;
    
    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
    }
    
    @Test
    @DisplayName("Should store and retrieve value")
    void testPutAndGet() {
        storage.put("key1", "value1");
        assertEquals("value1", storage.get("key1"));
    }
    
    @Test
    @DisplayName("Should return null for non-existent key")
    void testGetNonExistentKey() {
        assertNull(storage.get("nonexistent"));
    }
    
    @Test
    @DisplayName("Should update existing key")
    void testUpdateExistingKey() {
        storage.put("key1", "value1");
        storage.put("key1", "value2");
        assertEquals("value2", storage.get("key1"));
    }
    
    @Test
    @DisplayName("Should delete key")
    void testDelete() {
        storage.put("key1", "value1");
        storage.delete("key1");
        assertNull(storage.get("key1"));
    }
    
    @Test
    @DisplayName("Should handle delete on non-existent key")
    void testDeleteNonExistentKey() {
        assertDoesNotThrow(() -> storage.delete("nonexistent"));
    }
    
    @Test
    @DisplayName("Should clear all entries")
    void testClear() {
        storage.put("key1", "value1");
        storage.put("key2", "value2");
        storage.clear();
        assertEquals(0, storage.size());
        assertNull(storage.get("key1"));
        assertNull(storage.get("key2"));
    }
    
    @Test
    @DisplayName("Should check if key exists")
    void testContainsKey() {
        storage.put("key1", "value1");
        assertTrue(storage.containsKey("key1"));
        assertFalse(storage.containsKey("key2"));
    }
    
    @Test
    @DisplayName("Should return correct size")
    void testSize() {
        assertEquals(0, storage.size());
        storage.put("key1", "value1");
        assertEquals(1, storage.size());
        storage.put("key2", "value2");
        assertEquals(2, storage.size());
        storage.delete("key1");
        assertEquals(1, storage.size());
    }
    
    @Test
    @DisplayName("Should throw exception for null key on put")
    void testPutNullKey() {
        assertThrows(IllegalArgumentException.class, () -> storage.put(null, "value"));
    }
    
    @Test
    @DisplayName("Should throw exception for null value on put")
    void testPutNullValue() {
        assertThrows(IllegalArgumentException.class, () -> storage.put("key", null));
    }
    
    @Test
    @DisplayName("Should handle null key on get")
    void testGetNullKey() {
        assertNull(storage.get(null));
    }
    
    @Test
    @DisplayName("Should handle null key on delete")
    void testDeleteNullKey() {
        assertDoesNotThrow(() -> storage.delete(null));
    }
    
    @Test
    @DisplayName("Should handle null key on containsKey")
    void testContainsKeyNull() {
        assertFalse(storage.containsKey(null));
    }
    
    @Test
    @DisplayName("Should handle concurrent puts")
    void testConcurrentPuts() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        String value = "value-" + threadId + "-" + j;
                        storage.put(key, value);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals(threadCount * operationsPerThread, storage.size());
    }
    
    @Test
    @DisplayName("Should handle concurrent reads and writes")
    void testConcurrentReadsAndWrites() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // Pre-populate
        for (int i = 0; i < 100; i++) {
            storage.put("key-" + i, "value-" + i);
        }
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        if (threadId % 2 == 0) {
                            // Write
                            storage.put("key-" + j, "updated-" + threadId);
                        } else {
                            // Read
                            storage.get("key-" + j);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals(100, storage.size());
    }
}

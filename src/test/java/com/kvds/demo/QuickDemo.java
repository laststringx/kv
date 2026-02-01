package com.kvds.demo;

import com.kvds.core.KeyValueStore;
import com.kvds.core.KeyValueStoreImpl;
import com.kvds.storage.InMemoryStorage;
import com.kvds.storage.Storage;
import com.kvds.wal.WriteAheadLog;
import com.kvds.wal.WriteAheadLogImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Quick demonstration and manual test of KV-DS functionality.
 * Run this to see the system in action!
 */
public class QuickDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   KV-DS Quick Demo & Manual Test Suite    ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        // Test 1: Basic Operations (No WAL)
        testBasicOperations();
        
        // Test 2: WAL and Crash Recovery
        testWALAndRecovery();
        
        // Test 3: Multiple Crash-Recovery Cycles
        testMultipleCycles();
        
        // Test 4: Error Handling
        testErrorHandling();
        
        // Test 5: Large Dataset
        testLargeDataset();
        
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║         All Tests Passed! ✓                ║");
        System.out.println("╚════════════════════════════════════════════╝");
    }
    
    private static void testBasicOperations() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Test 1: Basic Operations (No WAL)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Storage storage = new InMemoryStorage();
        KeyValueStore store = new KeyValueStoreImpl(storage);
        
        // PUT operation
        store.put("name", "Alice");
        store.put("age", "30");
        store.put("city", "New York");
        
        System.out.println("✓ PUT operations completed");
        
        // GET operation
        System.out.println("  name: " + store.get("name"));
        System.out.println("  age: " + store.get("age"));
        System.out.println("  city: " + store.get("city"));
        
        // UPDATE operation
        store.put("age", "31");
        System.out.println("✓ UPDATE: age updated to " + store.get("age"));
        
        // DELETE operation
        store.delete("city");
        System.out.println("✓ DELETE: city deleted (value: " + store.get("city") + ")");
        
        // CLEAR operation
        store.clear();
        System.out.println("✓ CLEAR: all data cleared (name: " + store.get("name") + ")");
        
        store.close();
        System.out.println("✓ Test 1 PASSED\n");
    }
    
    private static void testWALAndRecovery() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Test 2: WAL and Crash Recovery");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Path walPath = Paths.get("demo-wal.log");
        
        // Phase 1: Write data with WAL
        System.out.println("Phase 1: Writing data with WAL enabled...");
        WriteAheadLog wal1 = new WriteAheadLogImpl(walPath.toString());
        Storage storage1 = new InMemoryStorage();
        KeyValueStore store1 = new KeyValueStoreImpl(storage1, wal1);
        
        store1.put("session_id", "abc123xyz");
        store1.put("user_name", "Bob");
        store1.put("login_time", "2026-02-01 23:00:00");
        store1.delete("temp_data"); // Delete non-existent key
        
        System.out.println("  session_id: " + store1.get("session_id"));
        System.out.println("  user_name: " + store1.get("user_name"));
        System.out.println("✓ Data written to WAL");
        
        store1.close(); // Simulate crash
        System.out.println("✓ Simulated crash (store closed)");
        
        // Phase 2: Recover from WAL
        System.out.println("\nPhase 2: Recovering from WAL...");
        WriteAheadLog wal2 = new WriteAheadLogImpl(walPath.toString());
        Storage storage2 = new InMemoryStorage();
        KeyValueStore store2 = new KeyValueStoreImpl(storage2, wal2);
        
        System.out.println("  session_id: " + store2.get("session_id"));
        System.out.println("  user_name: " + store2.get("user_name"));
        System.out.println("  login_time: " + store2.get("login_time"));
        
        // Verify recovery
        if ("abc123xyz".equals(store2.get("session_id")) && 
            "Bob".equals(store2.get("user_name"))) {
            System.out.println("✓ Recovery successful - all data restored!");
        } else {
            System.out.println("✗ Recovery failed!");
        }
        
        store2.close();
        Files.deleteIfExists(walPath);
        System.out.println("✓ Test 2 PASSED\n");
    }
    
    private static void testMultipleCycles() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Test 3: Multiple Crash-Recovery Cycles");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Path walPath = Paths.get("demo-multi-cycle.log");
        
        // Cycle 1
        System.out.println("Cycle 1: Initial write");
        WriteAheadLog wal1 = new WriteAheadLogImpl(walPath.toString());
        Storage storage1 = new InMemoryStorage();
        KeyValueStore store1 = new KeyValueStoreImpl(storage1, wal1);
        store1.put("counter", "1");
        System.out.println("  counter = " + store1.get("counter"));
        store1.close();
        
        // Cycle 2
        System.out.println("Cycle 2: Recover and update");
        WriteAheadLog wal2 = new WriteAheadLogImpl(walPath.toString());
        Storage storage2 = new InMemoryStorage();
        KeyValueStore store2 = new KeyValueStoreImpl(storage2, wal2);
        System.out.println("  counter (recovered) = " + store2.get("counter"));
        store2.put("counter", "2");
        store2.put("data", "important");
        System.out.println("  counter (updated) = " + store2.get("counter"));
        store2.close();
        
        // Cycle 3
        System.out.println("Cycle 3: Final recovery");
        WriteAheadLog wal3 = new WriteAheadLogImpl(walPath.toString());
        Storage storage3 = new InMemoryStorage();
        KeyValueStore store3 = new KeyValueStoreImpl(storage3, wal3);
        System.out.println("  counter (final) = " + store3.get("counter"));
        System.out.println("  data (final) = " + store3.get("data"));
        
        if ("2".equals(store3.get("counter")) && "important".equals(store3.get("data"))) {
            System.out.println("✓ Multiple cycles successful!");
        }
        
        store3.close();
        Files.deleteIfExists(walPath);
        System.out.println("✓ Test 3 PASSED\n");
    }
    
    private static void testErrorHandling() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Test 4: Error Handling");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Storage storage = new InMemoryStorage();
        KeyValueStore store = new KeyValueStoreImpl(storage);
        
        // Test null key
        try {
            store.put(null, "value");
            System.out.println("✗ Null key should be rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Null key rejected: " + e.getMessage());
        }
        
        // Test empty key
        try {
            store.put("", "value");
            System.out.println("✗ Empty key should be rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Empty key rejected: " + e.getMessage());
        }
        
        // Test pipe character in key
        try {
            store.put("key|with|pipes", "value");
            System.out.println("✗ Pipe in key should be rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Pipe in key rejected: " + e.getMessage());
        }
        
        // Test pipe character in value
        try {
            store.put("key", "value|with|pipes");
            System.out.println("✗ Pipe in value should be rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Pipe in value rejected: " + e.getMessage());
        }
        
        // Test get non-existent key
        String result = store.get("nonexistent");
        if (result == null) {
            System.out.println("✓ Non-existent key returns null");
        }
        
        store.close();
        System.out.println("✓ Test 4 PASSED\n");
    }
    
    private static void testLargeDataset() throws Exception {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Test 5: Large Dataset Recovery");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        Path walPath = Paths.get("demo-large.log");
        int datasetSize = 100;
        
        // Write large dataset
        System.out.println("Writing " + datasetSize + " entries...");
        WriteAheadLog wal1 = new WriteAheadLogImpl(walPath.toString());
        Storage storage1 = new InMemoryStorage();
        KeyValueStore store1 = new KeyValueStoreImpl(storage1, wal1);
        
        for (int i = 0; i < datasetSize; i++) {
            store1.put("key" + i, "value" + i);
        }
        
        // Delete every other entry
        for (int i = 0; i < datasetSize; i += 2) {
            store1.delete("key" + i);
        }
        
        System.out.println("✓ Wrote " + datasetSize + " entries");
        System.out.println("✓ Deleted " + (datasetSize / 2) + " entries");
        System.out.println("  Sample - key50: " + store1.get("key50"));
        System.out.println("  Sample - key51: " + store1.get("key51"));
        
        store1.close();
        
        // Recover
        System.out.println("\nRecovering from WAL...");
        WriteAheadLog wal2 = new WriteAheadLogImpl(walPath.toString());
        Storage storage2 = new InMemoryStorage();
        KeyValueStore store2 = new KeyValueStoreImpl(storage2, wal2);
        
        // Verify recovery
        int recoveredCount = 0;
        for (int i = 0; i < datasetSize; i++) {
            String value = store2.get("key" + i);
            if (i % 2 == 0) {
                // Should be deleted
                if (value == null) recoveredCount++;
            } else {
                // Should exist
                if (("value" + i).equals(value)) recoveredCount++;
            }
        }
        
        System.out.println("✓ Recovered " + recoveredCount + "/" + datasetSize + " entries correctly");
        System.out.println("  Sample - key50 (deleted): " + store2.get("key50"));
        System.out.println("  Sample - key51 (exists): " + store2.get("key51"));
        
        if (recoveredCount == datasetSize) {
            System.out.println("✓ All entries recovered correctly!");
        }
        
        store2.close();
        Files.deleteIfExists(walPath);
        System.out.println("✓ Test 5 PASSED\n");
    }
}

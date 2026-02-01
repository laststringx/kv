package com.kvds.core;

import com.kvds.storage.InMemoryStorage;
import com.kvds.storage.Storage;
import com.kvds.wal.WriteAheadLog;
import com.kvds.wal.WriteAheadLogImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for KeyValueStore with WAL and Recovery.
 * Tests the complete flow: write to WAL, crash simulation, and recovery.
 */
@DisplayName("KeyValueStore Integration Tests with WAL and Recovery")
class KeyValueStoreIntegrationTest {

    @TempDir
    Path tempDir;

    private Path walPath;
    private WriteAheadLog wal;
    private Storage storage;
    private KeyValueStore store;

    @BeforeEach
    void setUp() throws IOException {
        walPath = tempDir.resolve("test-wal.log");
        wal = new WriteAheadLogImpl(walPath.toString());
        storage = new InMemoryStorage();
        store = new KeyValueStoreImpl(storage, wal);
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    @Test
    @DisplayName("Should recover data after simulated crash")
    void testRecoveryAfterCrash() throws IOException {
        // Write some data
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.put("key3", "value3");

        // Verify data is there
        assertEquals("value1", store.get("key1"));
        assertEquals("value2", store.get("key2"));
        assertEquals("value3", store.get("key3"));

        // Simulate crash by closing and creating new instances
        store.close();

        // Create new instances (simulating restart after crash)
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Verify data was recovered
        assertEquals("value1", newStore.get("key1"));
        assertEquals("value2", newStore.get("key2"));
        assertEquals("value3", newStore.get("key3"));

        newStore.close();
    }

    @Test
    @DisplayName("Should recover after multiple operations including deletes")
    void testRecoveryWithDeletes() throws IOException {
        // Perform various operations
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.put("key3", "value3");
        store.delete("key2");
        store.put("key4", "value4");

        // Simulate crash
        store.close();

        // Recover
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Verify state
        assertEquals("value1", newStore.get("key1"));
        assertNull(newStore.get("key2")); // Was deleted
        assertEquals("value3", newStore.get("key3"));
        assertEquals("value4", newStore.get("key4"));

        newStore.close();
    }

    @Test
    @DisplayName("Should recover overwritten values correctly")
    void testRecoveryWithOverwrites() throws IOException {
        // Write and overwrite values
        store.put("key1", "value1");
        store.put("key1", "value2");
        store.put("key1", "value3");

        // Simulate crash
        store.close();

        // Recover
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Should have the latest value
        assertEquals("value3", newStore.get("key1"));

        newStore.close();
    }

    @Test
    @DisplayName("Should handle recovery from empty WAL")
    void testRecoveryFromEmptyWAL() throws IOException {
        // Close without writing anything
        store.close();

        // Recover
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Should be empty
        assertNull(newStore.get("key1"));

        newStore.close();
    }

    @Test
    @DisplayName("Should continue operations after recovery")
    void testOperationsAfterRecovery() throws IOException {
        // Initial operations
        store.put("key1", "value1");
        store.put("key2", "value2");

        // Simulate crash and recover
        store.close();
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Verify recovered data
        assertEquals("value1", newStore.get("key1"));
        assertEquals("value2", newStore.get("key2"));

        // Continue with new operations
        newStore.put("key3", "value3");
        newStore.delete("key1");

        // Verify new state
        assertNull(newStore.get("key1"));
        assertEquals("value2", newStore.get("key2"));
        assertEquals("value3", newStore.get("key3"));

        // Another crash and recovery
        newStore.close();
        WriteAheadLog finalWal = new WriteAheadLogImpl(walPath.toString());
        Storage finalStorage = new InMemoryStorage();
        KeyValueStore finalStore = new KeyValueStoreImpl(finalStorage, finalWal);

        // Verify final state
        assertNull(finalStore.get("key1"));
        assertEquals("value2", finalStore.get("key2"));
        assertEquals("value3", finalStore.get("key3"));

        finalStore.close();
    }

    @Test
    @DisplayName("Should handle large number of operations and recover")
    void testRecoveryWithManyOperations() throws IOException {
        // Write many entries
        for (int i = 0; i < 100; i++) {
            store.put("key" + i, "value" + i);
        }

        // Delete some
        for (int i = 0; i < 100; i += 2) {
            store.delete("key" + i);
        }

        // Simulate crash
        store.close();

        // Recover
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Verify state
        for (int i = 0; i < 100; i++) {
            if (i % 2 == 0) {
                assertNull(newStore.get("key" + i), "key" + i + " should be deleted");
            } else {
                assertEquals("value" + i, newStore.get("key" + i), "key" + i + " should exist");
            }
        }

        newStore.close();
    }

    @Test
    @DisplayName("Should handle put-delete-put sequence correctly")
    void testPutDeletePutSequence() throws IOException {
        // Sequence of operations
        store.put("key1", "value1");
        store.delete("key1");
        store.put("key1", "value2");

        // Simulate crash
        store.close();

        // Recover
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Should have the final value
        assertEquals("value2", newStore.get("key1"));

        newStore.close();
    }

    @Test
    @DisplayName("Should maintain data integrity across multiple crash-recovery cycles")
    void testMultipleCrashRecoveryCycles() throws IOException {
        // Cycle 1
        store.put("key1", "value1");
        store.close();

        // Cycle 2
        WriteAheadLog wal2 = new WriteAheadLogImpl(walPath.toString());
        Storage storage2 = new InMemoryStorage();
        KeyValueStore store2 = new KeyValueStoreImpl(storage2, wal2);
        assertEquals("value1", store2.get("key1"));
        store2.put("key2", "value2");
        store2.close();

        // Cycle 3
        WriteAheadLog wal3 = new WriteAheadLogImpl(walPath.toString());
        Storage storage3 = new InMemoryStorage();
        KeyValueStore store3 = new KeyValueStoreImpl(storage3, wal3);
        assertEquals("value1", store3.get("key1"));
        assertEquals("value2", store3.get("key2"));
        store3.put("key3", "value3");
        store3.close();

        // Final verification
        WriteAheadLog walFinal = new WriteAheadLogImpl(walPath.toString());
        Storage storageFinal = new InMemoryStorage();
        KeyValueStore storeFinal = new KeyValueStoreImpl(storageFinal, walFinal);
        assertEquals("value1", storeFinal.get("key1"));
        assertEquals("value2", storeFinal.get("key2"));
        assertEquals("value3", storeFinal.get("key3"));
        storeFinal.close();
    }

    @Test
    @DisplayName("Should handle recovery with special characters in values")
    void testRecoveryWithSpecialCharacters() throws IOException {
        // Note: pipe character and newlines are not allowed due to WAL format
        store.put("key1", "value with spaces");
        store.put("key2", "value\twith\ttabs");
        store.put("key3", "value-with-dashes");

        // Simulate crash
        store.close();

        // Recover
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        Storage newStorage = new InMemoryStorage();
        KeyValueStore newStore = new KeyValueStoreImpl(newStorage, newWal);

        // Verify
        assertEquals("value with spaces", newStore.get("key1"));
        assertEquals("value\twith\ttabs", newStore.get("key2"));
        assertEquals("value-with-dashes", newStore.get("key3"));

        newStore.close();
    }

    @Test
    @DisplayName("Should throw exception when creating store with null WAL")
    void testCreateStoreWithNullWAL() {
        assertThrows(IllegalArgumentException.class, () -> {
            new KeyValueStoreImpl(storage, null);
        });
    }

    @Test
    @DisplayName("Should throw exception when creating store with null storage")
    void testCreateStoreWithNullStorage() throws IOException {
        WriteAheadLog testWal = new WriteAheadLogImpl(walPath.toString());
        assertThrows(IllegalArgumentException.class, () -> {
            new KeyValueStoreImpl(null, testWal);
        });
        testWal.close();
    }
}

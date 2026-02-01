package com.kvds.wal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WriteAheadLogImpl Tests")
class WriteAheadLogImplTest {
    
    @TempDir
    Path tempDir;
    
    private WriteAheadLog wal;
    private String walPath;
    
    @BeforeEach
    void setUp() {
        walPath = tempDir.resolve("test-wal.log").toString();
        wal = new WriteAheadLogImpl(walPath);
    }
    
    @AfterEach
    void tearDown() {
        if (wal != null) {
            wal.close();
        }
    }
    
    @Test
    @DisplayName("Should create WAL file")
    void testCreateWalFile() {
        assertTrue(Files.exists(Path.of(walPath)));
    }
    
    @Test
    @DisplayName("Should append and read single entry")
    void testAppendAndReadSingleEntry() {
        LogEntry entry = new LogEntry(Operation.PUT, "key1", "value1");
        wal.append(entry);
        
        List<LogEntry> entries = wal.readAll();
        assertEquals(1, entries.size());
        assertEquals("key1", entries.get(0).getKey());
        assertEquals("value1", entries.get(0).getValue());
        assertEquals(Operation.PUT, entries.get(0).getOperation());
    }
    
    @Test
    @DisplayName("Should append and read multiple entries")
    void testAppendAndReadMultipleEntries() {
        wal.append(new LogEntry(Operation.PUT, "key1", "value1"));
        wal.append(new LogEntry(Operation.PUT, "key2", "value2"));
        wal.append(new LogEntry(Operation.DELETE, "key1", null));
        
        List<LogEntry> entries = wal.readAll();
        assertEquals(3, entries.size());
        
        assertEquals(Operation.PUT, entries.get(0).getOperation());
        assertEquals("key1", entries.get(0).getKey());
        
        assertEquals(Operation.PUT, entries.get(1).getOperation());
        assertEquals("key2", entries.get(1).getKey());
        
        assertEquals(Operation.DELETE, entries.get(2).getOperation());
        assertEquals("key1", entries.get(2).getKey());
        assertNull(entries.get(2).getValue());
    }
    
    @Test
    @DisplayName("Should handle DELETE operations")
    void testDeleteOperation() {
        LogEntry entry = new LogEntry(Operation.DELETE, "key1", null);
        wal.append(entry);
        
        List<LogEntry> entries = wal.readAll();
        assertEquals(1, entries.size());
        assertEquals(Operation.DELETE, entries.get(0).getOperation());
        assertNull(entries.get(0).getValue());
    }
    
    @Test
    @DisplayName("Should return empty list when WAL is empty")
    void testReadEmptyWal() {
        List<LogEntry> entries = wal.readAll();
        assertTrue(entries.isEmpty());
    }
    
    @Test
    @DisplayName("Should throw exception for null entry")
    void testAppendNullEntry() {
        assertThrows(IllegalArgumentException.class, () -> wal.append(null));
    }
    
    @Test
    @DisplayName("Should persist entries across instances")
    void testPersistenceAcrossInstances() {
        wal.append(new LogEntry(Operation.PUT, "key1", "value1"));
        wal.append(new LogEntry(Operation.PUT, "key2", "value2"));
        wal.close();
        
        // Create new instance
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath);
        List<LogEntry> entries = newWal.readAll();
        
        assertEquals(2, entries.size());
        assertEquals("key1", entries.get(0).getKey());
        assertEquals("key2", entries.get(1).getKey());
        
        newWal.close();
    }
    
    @Test
    @DisplayName("Should clear WAL")
    void testClear() throws IOException {
        wal.append(new LogEntry(Operation.PUT, "key1", "value1"));
        wal.clear();
        
        assertFalse(Files.exists(Path.of(walPath)));
    }
    
    @Test
    @DisplayName("Should handle values with spaces")
    void testValuesWithSpaces() {
        LogEntry entry = new LogEntry(Operation.PUT, "key1", "value with spaces");
        wal.append(entry);
        
        List<LogEntry> entries = wal.readAll();
        assertEquals(1, entries.size());
        assertEquals("value with spaces", entries.get(0).getValue());
    }
    
    @Test
    @DisplayName("Should handle special characters in values")
    void testSpecialCharacters() {
        LogEntry entry = new LogEntry(Operation.PUT, "key1", "value@#$%^&*()");
        wal.append(entry);
        
        List<LogEntry> entries = wal.readAll();
        assertEquals(1, entries.size());
        assertEquals("value@#$%^&*()", entries.get(0).getValue());
    }
    
    @Test
    @DisplayName("Should create parent directories")
    void testCreateParentDirectories() {
        String nestedPath = tempDir.resolve("nested/dir/wal.log").toString();
        WriteAheadLog nestedWal = new WriteAheadLogImpl(nestedPath);
        
        assertTrue(Files.exists(Path.of(nestedPath)));
        nestedWal.close();
    }
    
    @Test
    @DisplayName("Should handle corrupted entries gracefully")
    void testCorruptedEntries() throws IOException {
        // Write some valid entries
        wal.append(new LogEntry(Operation.PUT, "key1", "value1"));
        wal.close();
        
        // Manually append corrupted entry
        Files.writeString(Path.of(walPath), "corrupted|entry\n", 
            java.nio.file.StandardOpenOption.APPEND);
        
        // Write another valid entry
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath);
        newWal.append(new LogEntry(Operation.PUT, "key2", "value2"));
        
        // Should skip corrupted entry and read valid ones
        List<LogEntry> entries = newWal.readAll();
        assertEquals(2, entries.size());
        assertEquals("key1", entries.get(0).getKey());
        assertEquals("key2", entries.get(1).getKey());
        
        newWal.close();
    }
    
    @Test
    @DisplayName("Should handle concurrent appends")
    void testConcurrentAppends() throws InterruptedException {
        int threadCount = 5;
        int entriesPerThread = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < entriesPerThread; j++) {
                    String key = "key-" + threadId + "-" + j;
                    String value = "value-" + threadId + "-" + j;
                    wal.append(new LogEntry(Operation.PUT, key, value));
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        List<LogEntry> entries = wal.readAll();
        assertEquals(threadCount * entriesPerThread, entries.size());
    }
}

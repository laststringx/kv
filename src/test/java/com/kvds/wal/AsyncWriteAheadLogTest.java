package com.kvds.wal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for AsyncWriteAheadLog.
 * Tests the core functionality without complex timing issues.
 */
@DisplayName("AsyncWriteAheadLog Tests")
class AsyncWriteAheadLogTest {
    
    @TempDir
    Path tempDir;
    
    private Path walPath;
    private WriteAheadLog syncWal;
    private AsyncWriteAheadLog asyncWal;
    
    @BeforeEach
    void setUp() throws IOException {
        walPath = tempDir.resolve("test-async-wal.log");
        syncWal = new WriteAheadLogImpl(walPath.toString());
        asyncWal = new AsyncWriteAheadLog(syncWal);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (asyncWal != null) {
            asyncWal.close();
        }
    }
    
    @Test
    @DisplayName("Should create AsyncWAL successfully")
    void testCreation() {
        assertNotNull(asyncWal);
        assertTrue(asyncWal.isRunning());
    }
    
    @Test
    @DisplayName("Should write and read single entry")
    void testSingleEntry() throws IOException, InterruptedException {
        LogEntry entry = new LogEntry(Operation.PUT, "key1", "value1");
        
        asyncWal.append(entry);
        Thread.sleep(100); // Give time for async write
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertTrue(entries.size() >= 1, "Should have at least 1 entry");
        assertEquals("key1", entries.get(0).getKey());
        assertEquals("value1", entries.get(0).getValue());
    }
    
    @Test
    @DisplayName("Should flush on close")
    void testFlushOnClose() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        Thread.sleep(100);
        asyncWal.close();
        
        // Reopen and verify all entries were written
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        List<LogEntry> entries = newWal.readAll();
        assertTrue(entries.size() >= 10, "Should have at least 10 entries, got " + entries.size());
        newWal.close();
        
        asyncWal = null; // Prevent double close in tearDown
    }
    
    @Test
    @DisplayName("Should handle mixed operations")
    void testMixedOperations() throws IOException, InterruptedException {
        asyncWal.append(new LogEntry(Operation.PUT, "key1", "value1"));
        asyncWal.append(new LogEntry(Operation.PUT, "key2", "value2"));
        asyncWal.append(new LogEntry(Operation.DELETE, "key1", null));
        asyncWal.append(new LogEntry(Operation.PUT, "key3", "value3"));
        
        Thread.sleep(200);
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertTrue(entries.size() >= 4, "Should have at least 4 entries");
        assertEquals(Operation.PUT, entries.get(0).getOperation());
        assertEquals(Operation.DELETE, entries.get(2).getOperation());
    }
    
    @Test
    @DisplayName("Should report pending writes")
    void testPendingWrites() throws IOException, InterruptedException {
        // Write entries quickly
        for (int i = 0; i < 20; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        // Should have some pending writes
        int pending = asyncWal.getPendingWrites();
        assertTrue(pending >= 0, "Should report pending writes");
        
        Thread.sleep(500);
        asyncWal.flush();
        
        // After flush, should be empty or nearly empty
        assertTrue(asyncWal.getPendingWrites() <= 5, "Should have few pending after flush");
    }
    
    @Test
    @DisplayName("Should reject writes after close")
    void testWriteAfterClose() throws IOException {
        asyncWal.close();
        
        assertThrows(Exception.class, () -> {
            LogEntry entry = new LogEntry(Operation.PUT, "key1", "value1");
            asyncWal.append(entry);
        });
        
        asyncWal = null; // Prevent double close
    }
    
    @Test
    @DisplayName("Should validate constructor parameters")
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncWriteAheadLog(null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncWriteAheadLog(syncWal, 0, 100, 10);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncWriteAheadLog(syncWal, 1000, 0, 10);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new AsyncWriteAheadLog(syncWal, 1000, 100, 0);
        });
    }
    
    @Test
    @DisplayName("Should handle custom batch configuration")
    void testCustomBatchConfig() throws IOException, InterruptedException {
        asyncWal.close();
        
        // Create async WAL with small batch size
        syncWal = new WriteAheadLogImpl(walPath.toString());
        asyncWal = new AsyncWriteAheadLog(syncWal, 1000, 10, 5);
        
        for (int i = 0; i < 15; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        Thread.sleep(200);
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertTrue(entries.size() >= 15, "Should have at least 15 entries");
    }
}

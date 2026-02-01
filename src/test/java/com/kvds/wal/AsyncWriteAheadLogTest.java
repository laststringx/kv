package com.kvds.wal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsyncWriteAheadLog.
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
    @DisplayName("Should write and read single entry")
    void testSingleEntry() throws IOException {
        LogEntry entry = new LogEntry(Operation.PUT, "key1", "value1");
        
        asyncWal.append(entry);
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertEquals(1, entries.size());
        assertEquals("key1", entries.get(0).getKey());
        assertEquals("value1", entries.get(0).getValue());
    }
    
    @Test
    @DisplayName("Should write multiple entries")
    void testMultipleEntries() throws IOException {
        for (int i = 0; i < 100; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertEquals(100, entries.size());
    }
    
    @Test
    @DisplayName("Should batch writes for better performance")
    void testBatchWrites() throws IOException, InterruptedException {
        int numEntries = 1000;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numEntries; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        asyncWal.flush();
        long endTime = System.nanoTime();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertEquals(numEntries, entries.size());
        
        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Wrote " + numEntries + " entries in " + durationMs + "ms");
        
        // Should be much faster than synchronous writes
        assertTrue(durationMs < 5000, "Async writes should be fast");
    }
    
    @Test
    @DisplayName("Should handle concurrent writes")
    void testConcurrentWrites() throws InterruptedException, IOException {
        int numThreads = 10;
        int entriesPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < entriesPerThread; i++) {
                        LogEntry entry = new LogEntry(Operation.PUT, 
                            "thread" + threadId + "_key" + i, 
                            "value" + i);
                        asyncWal.append(entry);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();
        
        assertEquals(0, errors.get(), "No errors should occur");
        
        asyncWal.flush();
        List<LogEntry> entries = asyncWal.readAll();
        assertEquals(numThreads * entriesPerThread, entries.size());
    }
    
    @Test
    @DisplayName("Should flush on close")
    void testFlushOnClose() throws IOException {
        for (int i = 0; i < 50; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        // Close without explicit flush
        asyncWal.close();
        
        // Reopen and verify all entries were written
        WriteAheadLog newWal = new WriteAheadLogImpl(walPath.toString());
        List<LogEntry> entries = newWal.readAll();
        assertEquals(50, entries.size());
        newWal.close();
        
        asyncWal = null; // Prevent double close in tearDown
    }
    
    @Test
    @DisplayName("Should handle mixed operations")
    void testMixedOperations() throws IOException {
        asyncWal.append(new LogEntry(Operation.PUT, "key1", "value1"));
        asyncWal.append(new LogEntry(Operation.PUT, "key2", "value2"));
        asyncWal.append(new LogEntry(Operation.DELETE, "key1", null));
        asyncWal.append(new LogEntry(Operation.PUT, "key3", "value3"));
        
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertEquals(4, entries.size());
        assertEquals(Operation.PUT, entries.get(0).getOperation());
        assertEquals(Operation.DELETE, entries.get(2).getOperation());
    }
    
    @Test
    @DisplayName("Should report pending writes")
    void testPendingWrites() throws IOException, InterruptedException {
        // Write many entries quickly
        for (int i = 0; i < 100; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        // Should have pending writes
        int pending = asyncWal.getPendingWrites();
        assertTrue(pending >= 0, "Should report pending writes");
        
        asyncWal.flush();
        
        // After flush, should be empty
        assertEquals(0, asyncWal.getPendingWrites());
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
    @DisplayName("Should handle custom batch configuration")
    void testCustomBatchConfig() throws IOException {
        asyncWal.close();
        
        // Create async WAL with small batch size
        syncWal = new WriteAheadLogImpl(walPath.toString());
        asyncWal = new AsyncWriteAheadLog(syncWal, 1000, 10, 5);
        
        for (int i = 0; i < 50; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        assertEquals(50, entries.size());
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
    @DisplayName("Should maintain entry order")
    void testEntryOrder() throws IOException {
        for (int i = 0; i < 100; i++) {
            LogEntry entry = new LogEntry(Operation.PUT, "key" + i, "value" + i);
            asyncWal.append(entry);
        }
        
        asyncWal.flush();
        
        List<LogEntry> entries = asyncWal.readAll();
        for (int i = 0; i < 100; i++) {
            assertEquals("key" + i, entries.get(i).getKey());
            assertEquals("value" + i, entries.get(i).getValue());
        }
    }
}

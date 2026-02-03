package com.kvds.wal;

import com.kvds.exception.KVDSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronous Write-Ahead Log implementation.
 * Batches writes in memory and flushes them asynchronously to improve throughput.
 * 
 * Features:
 * - Async writes with background thread
 * - Batch commits for higher throughput
 * - Configurable batch size and timeout
 * - Graceful shutdown with flush
 */
public class AsyncWriteAheadLog implements WriteAheadLog {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncWriteAheadLog.class);
    
    private final WriteAheadLog delegate; // Underlying synchronous WAL
    private final BlockingQueue<LogEntry> writeQueue;
    private final ExecutorService writerThread;
    private final AtomicBoolean running;
    
    // Configuration
    private final int batchSize;
    private final long batchTimeoutMs;
    
    // Default configuration
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_BATCH_TIMEOUT_MS = 10; // 10ms
    
    /**
     * Creates an async WAL with default configuration.
     */
    public AsyncWriteAheadLog(WriteAheadLog delegate) {
        this(delegate, DEFAULT_QUEUE_SIZE, DEFAULT_BATCH_SIZE, DEFAULT_BATCH_TIMEOUT_MS);
    }
    
    /**
     * Creates an async WAL with custom configuration.
     * 
     * @param delegate Underlying synchronous WAL
     * @param queueSize Maximum number of pending writes
     * @param batchSize Number of entries to batch before flushing
     * @param batchTimeoutMs Maximum time to wait before flushing (milliseconds)
     */
    public AsyncWriteAheadLog(WriteAheadLog delegate, int queueSize, int batchSize, long batchTimeoutMs) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate WAL cannot be null");
        }
        if (queueSize <= 0 || batchSize <= 0 || batchTimeoutMs <= 0) {
            throw new IllegalArgumentException("Queue size, batch size, and timeout must be positive");
        }
        
        this.delegate = delegate;
        this.writeQueue = new LinkedBlockingQueue<>(queueSize);
        this.batchSize = batchSize;
        this.batchTimeoutMs = batchTimeoutMs;
        this.running = new AtomicBoolean(true);
        
        // Start background writer thread
        this.writerThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AsyncWAL-Writer");
            t.setDaemon(false); // Ensure proper shutdown
            return t;
        });
        
        writerThread.submit(this::writerLoop);
        
        logger.info("AsyncWriteAheadLog started: queueSize={}, batchSize={}, batchTimeoutMs={}", 
                    queueSize, batchSize, batchTimeoutMs);
    }
    
    @Override
    public void append(LogEntry entry) {
        if (!running.get()) {
            throw new KVDSException("AsyncWAL is closed");
        }
        
        try {
            // Try to add to queue with timeout
            boolean added = writeQueue.offer(entry, 1, TimeUnit.SECONDS);
            if (!added) {
                // Queue is full - apply backpressure
                logger.warn("Write queue full, blocking until space available");
                writeQueue.put(entry); // Block until space available
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KVDSException("Interrupted while appending to WAL", e);
        }
    }
    
    @Override
    public List<LogEntry> readAll() {
        // Flush pending writes before reading
        flush();
        return delegate.readAll();
    }    
    @Override
    public void clear() {
        // Flush pending writes before clearing
        flush();
        // Clear the queue
        writeQueue.clear();
        // Clear the underlying WAL
        delegate.clear();
    }

    
    @Override
    public void close() {
        logger.info("Closing AsyncWriteAheadLog...");
        
        // Stop accepting new writes
        running.set(false);
        
        // Flush all pending writes
        flush();
        
        // Shutdown writer thread
        writerThread.shutdown();
        try {
            if (!writerThread.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Writer thread did not terminate in time, forcing shutdown");
                writerThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writerThread.shutdownNow();
        }
        
        // Close underlying WAL
        delegate.close();
        
        logger.info("AsyncWriteAheadLog closed");
    }
    
    /**
     * Flushes all pending writes to the underlying WAL.
     */
    public void flush() {
        logger.debug("Flushing {} pending entries", writeQueue.size());
        
        // Drain all pending entries
        while (!writeQueue.isEmpty()) {
            try {
                Thread.sleep(1); // Give writer thread time to process
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new KVDSException("Interrupted while flushing", e);
            }
        }
    }
    
    /**
     * Background writer loop that batches and writes entries.
     */
    private void writerLoop() {
        logger.info("AsyncWAL writer thread started");
        
        while (running.get() || !writeQueue.isEmpty()) {
            try {
                writeBatch();
            } catch (Exception e) {
                logger.error("Error in writer loop", e);
                // Continue processing to avoid losing data
            }
        }
        
        logger.info("AsyncWAL writer thread stopped");
    }
    
    /**
     * Writes a batch of entries to the underlying WAL.
     */
    private void writeBatch() {
        // Collect entries for batch
        List<LogEntry> batch = new java.util.ArrayList<>(batchSize);
        
        try {
            // Wait for first entry (with timeout)
            LogEntry first = writeQueue.poll(batchTimeoutMs, TimeUnit.MILLISECONDS);
            if (first == null) {
                return; // Timeout, no entries available
            }
            batch.add(first);
            
            // Collect additional entries up to batch size (non-blocking)
            writeQueue.drainTo(batch, batchSize - 1);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        // Write batch to underlying WAL
        if (!batch.isEmpty()) {
            for (LogEntry entry : batch) {
                delegate.append(entry);
            }
            logger.debug("Wrote batch of {} entries", batch.size());
        }
    }
    
    /**
     * Returns the number of pending writes in the queue.
     */
    public int getPendingWrites() {
        return writeQueue.size();
    }
    
    /**
     * Returns true if the async WAL is running.
     */
    public boolean isRunning() {
        return running.get();
    }
}

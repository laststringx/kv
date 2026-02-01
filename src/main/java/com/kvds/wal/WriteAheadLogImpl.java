package com.kvds.wal;

import com.kvds.exception.KVDSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based implementation of WriteAheadLog.
 * 
 * This class follows the Single Responsibility Principle (SRP)
 * by focusing solely on WAL file operations.
 * 
 * Thread-safety: append() is synchronized to ensure only one thread
 * writes to the WAL at a time.
 */
public class WriteAheadLogImpl implements WriteAheadLog {
    
    private static final Logger logger = LoggerFactory.getLogger(WriteAheadLogImpl.class);
    
    private final String logFilePath;
    private final BufferedWriter writer;
    private final FileOutputStream fileOutputStream;
    
    /**
     * Creates a new WriteAheadLog with the specified file path.
     * Creates parent directories if they don't exist.
     * 
     * @param logFilePath the path to the WAL file
     * @throws KVDSException if initialization fails
     */
    public WriteAheadLogImpl(String logFilePath) {
        this.logFilePath = logFilePath;
        
        try {
            // Create parent directories if they don't exist
            Path path = Paths.get(logFilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            
            // Open file in append mode
            this.fileOutputStream = new FileOutputStream(logFilePath, true);
            this.writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            
            logger.info("WriteAheadLog initialized: {}", logFilePath);
        } catch (IOException e) {
            logger.error("Failed to initialize WAL: {}", logFilePath, e);
            throw new KVDSException("Failed to initialize WAL: " + logFilePath, e);
        }
    }
    
    @Override
    public synchronized void append(LogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Log entry cannot be null");
        }
        
        try {
            String line = entry.serialize();
            writer.write(line);
            writer.newLine();
            writer.flush();
            
            // Force fsync for durability
            fileOutputStream.getFD().sync();
            
            logger.trace("Appended to WAL: {}", entry);
        } catch (IOException e) {
            logger.error("Failed to append to WAL", e);
            throw new KVDSException("Failed to append to WAL", e);
        }
    }
    
    @Override
    public List<LogEntry> readAll() {
        List<LogEntry> entries = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                try {
                    LogEntry entry = LogEntry.deserialize(line);
                    entries.add(entry);
                } catch (IllegalArgumentException e) {
                    // Log warning and skip corrupted entry
                    logger.warn("Skipping corrupted log entry at line {}: {}", lineNumber, line, e);
                }
            }
            
            logger.debug("Read {} entries from WAL", entries.size());
        } catch (FileNotFoundException e) {
            // WAL file doesn't exist yet - this is normal on first run
            logger.info("WAL file not found (first run): {}", logFilePath);
        } catch (IOException e) {
            logger.error("Failed to read WAL", e);
            throw new KVDSException("Failed to read WAL", e);
        }
        
        return entries;
    }
    
    @Override
    public void clear() {
        try {
            // Close current writer
            writer.close();
            fileOutputStream.close();
            
            // Delete the file
            Files.deleteIfExists(Paths.get(logFilePath));
            
            logger.info("WAL cleared: {}", logFilePath);
        } catch (IOException e) {
            logger.error("Failed to clear WAL", e);
            throw new KVDSException("Failed to clear WAL", e);
        }
    }
    
    @Override
    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            logger.info("WAL closed: {}", logFilePath);
        } catch (IOException e) {
            logger.error("Failed to close WAL", e);
            throw new KVDSException("Failed to close WAL", e);
        }
    }
}

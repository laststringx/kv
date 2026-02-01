package com.kvds.recovery;

import com.kvds.exception.KVDSException;
import com.kvds.storage.InMemoryStorage;
import com.kvds.storage.Storage;
import com.kvds.wal.LogEntry;
import com.kvds.wal.Operation;
import com.kvds.wal.WriteAheadLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecoveryManagerImpl.
 */
class RecoveryManagerImplTest {

    private RecoveryManager recoveryManager;
    private Storage storage;
    private WriteAheadLog mockWal;

    @BeforeEach
    void setUp() {
        recoveryManager = new RecoveryManagerImpl();
        storage = new InMemoryStorage();
        mockWal = mock(WriteAheadLog.class);
    }

    @Test
    void testRecoverWithEmptyWAL() {
        // Arrange
        when(mockWal.readAll()).thenReturn(new ArrayList<>());

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        verify(mockWal, times(1)).readAll();
        assertNull(storage.get("anykey")); // Storage should be empty
    }

    @Test
    void testRecoverSinglePutOperation() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertEquals("value1", storage.get("key1"));
    }

    @Test
    void testRecoverMultiplePutOperations() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        entries.add(new LogEntry(Operation.PUT, "key2", "value2"));
        entries.add(new LogEntry(Operation.PUT, "key3", "value3"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertEquals("value1", storage.get("key1"));
        assertEquals("value2", storage.get("key2"));
        assertEquals("value3", storage.get("key3"));
    }

    @Test
    void testRecoverDeleteOperation() {
        // Arrange
        storage.put("key1", "value1");
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.DELETE, "key1", null));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertNull(storage.get("key1"));
    }

    @Test
    void testRecoverMixedOperations() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        entries.add(new LogEntry(Operation.PUT, "key2", "value2"));
        entries.add(new LogEntry(Operation.DELETE, "key1", null));
        entries.add(new LogEntry(Operation.PUT, "key3", "value3"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertNull(storage.get("key1")); // Deleted
        assertEquals("value2", storage.get("key2"));
        assertEquals("value3", storage.get("key3"));
    }

    @Test
    void testRecoverOverwriteValue() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        entries.add(new LogEntry(Operation.PUT, "key1", "value2"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertEquals("value2", storage.get("key1")); // Should have latest value
    }

    @Test
    void testRecoverWithNullWAL() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            recoveryManager.recover(null, storage);
        });
    }

    @Test
    void testRecoverWithNullStorage() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            recoveryManager.recover(mockWal, null);
        });
    }

    @Test
    void testRecoverWithWALReadException() {
        // Arrange
        when(mockWal.readAll()).thenThrow(new RuntimeException("WAL read error"));

        // Act & Assert
        assertThrows(KVDSException.class, () -> {
            recoveryManager.recover(mockWal, storage);
        });
    }

    @Test
    void testRecoverContinuesOnEntryError() {
        // Arrange
        Storage mockStorage = mock(Storage.class);
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        entries.add(new LogEntry(Operation.PUT, "key2", "value2"));
        entries.add(new LogEntry(Operation.PUT, "key3", "value3"));
        
        when(mockWal.readAll()).thenReturn(entries);
        
        // Make the second entry fail
        doNothing().when(mockStorage).put("key1", "value1");
        doThrow(new RuntimeException("Storage error")).when(mockStorage).put("key2", "value2");
        doNothing().when(mockStorage).put("key3", "value3");

        // Act
        recoveryManager.recover(mockWal, mockStorage);

        // Assert - should have attempted all three operations
        verify(mockStorage, times(1)).put("key1", "value1");
        verify(mockStorage, times(1)).put("key2", "value2");
        verify(mockStorage, times(1)).put("key3", "value3");
    }

    @Test
    void testRecoverLargeNumberOfEntries() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entries.add(new LogEntry(Operation.PUT, "key" + i, "value" + i));
        }
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        for (int i = 0; i < 1000; i++) {
            assertEquals("value" + i, storage.get("key" + i));
        }
    }

    @Test
    void testRecoverDeleteNonExistentKey() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.DELETE, "nonexistent", null));
        when(mockWal.readAll()).thenReturn(entries);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            recoveryManager.recover(mockWal, storage);
        });
    }

    @Test
    void testRecoverPreservesExistingData() {
        // Arrange
        storage.put("existing", "data");
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertEquals("data", storage.get("existing")); // Existing data preserved
        assertEquals("value1", storage.get("key1")); // New data added
    }

    @Test
    void testRecoverWithSpecialCharactersInValues() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value|with|pipes"));
        entries.add(new LogEntry(Operation.PUT, "key2", "value\nwith\nnewlines"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertEquals("value|with|pipes", storage.get("key1"));
        assertEquals("value\nwith\nnewlines", storage.get("key2"));
    }

    @Test
    void testRecoverSequenceOfPutAndDelete() {
        // Arrange
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(Operation.PUT, "key1", "value1"));
        entries.add(new LogEntry(Operation.DELETE, "key1", null));
        entries.add(new LogEntry(Operation.PUT, "key1", "value2"));
        when(mockWal.readAll()).thenReturn(entries);

        // Act
        recoveryManager.recover(mockWal, storage);

        // Assert
        assertEquals("value2", storage.get("key1")); // Should have final value
    }
}

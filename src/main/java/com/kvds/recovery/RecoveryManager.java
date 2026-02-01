package com.kvds.recovery;

import com.kvds.storage.Storage;
import com.kvds.wal.WriteAheadLog;

/**
 * RecoveryManager interface for recovering state from WAL.
 * 
 * This interface follows the Interface Segregation Principle (ISP)
 * by providing only recovery-related operations.
 */
public interface RecoveryManager {
    
    /**
     * Recovers the storage state by replaying the WAL.
     * 
     * @param wal the write-ahead log to replay
     * @param storage the storage to recover
     * @throws com.kvds.exception.KVDSException if recovery fails
     */
    void recover(WriteAheadLog wal, Storage storage);
}

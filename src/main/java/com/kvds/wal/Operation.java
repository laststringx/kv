package com.kvds.wal;

/**
 * Enum representing WAL operations.
 */
public enum Operation {
    /**
     * PUT operation - insert or update a key-value pair
     */
    PUT,
    
    /**
     * DELETE operation - remove a key-value pair
     */
    DELETE
}

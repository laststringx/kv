package com.kvds.exception;

/**
 * Custom exception for KV-DS operations.
 * Extends RuntimeException to avoid forcing clients to handle checked exceptions.
 */
public class KVDSException extends RuntimeException {
    
    /**
     * Creates a new KVDSException with the specified message.
     * 
     * @param message the error message
     */
    public KVDSException(String message) {
        super(message);
    }
    
    /**
     * Creates a new KVDSException with the specified message and cause.
     * 
     * @param message the error message
     * @param cause the underlying cause
     */
    public KVDSException(String message, Throwable cause) {
        super(message, cause);
    }
}

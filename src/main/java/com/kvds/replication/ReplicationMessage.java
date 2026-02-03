package com.kvds.replication;

import com.kvds.wal.Operation;

import java.io.Serializable;

/**
 * Message format for replicating operations between nodes.
 */
public class ReplicationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final MessageType type;
    private final String sourceNodeId;
    private final Operation operation;
    private final String key;
    private final String value;
    private final long timestamp;
    
    public enum MessageType {
        REPLICATE_PUT,
        REPLICATE_DELETE,
        HEARTBEAT,
        SYNC_REQUEST,
        SYNC_RESPONSE,
        FAILOVER_NOTIFICATION
    }
    
    private ReplicationMessage(Builder builder) {
        this.type = builder.type;
        this.sourceNodeId = builder.sourceNodeId;
        this.operation = builder.operation;
        this.key = builder.key;
        this.value = builder.value;
        this.timestamp = builder.timestamp;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public String getSourceNodeId() {
        return sourceNodeId;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private MessageType type;
        private String sourceNodeId;
        private Operation operation;
        private String key;
        private String value;
        private long timestamp = System.currentTimeMillis();
        
        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }
        
        public Builder sourceNodeId(String sourceNodeId) {
            this.sourceNodeId = sourceNodeId;
            return this;
        }
        
        public Builder operation(Operation operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder value(String value) {
            this.value = value;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public ReplicationMessage build() {
            return new ReplicationMessage(this);
        }
    }
    
    @Override
    public String toString() {
        return "ReplicationMessage{" +
                "type=" + type +
                ", sourceNodeId='" + sourceNodeId + '\'' +
                ", operation=" + operation +
                ", key='" + key + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

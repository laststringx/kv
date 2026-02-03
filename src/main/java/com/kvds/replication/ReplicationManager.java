package com.kvds.replication;

import com.kvds.wal.Operation;

import java.util.List;

/**
 * Interface for managing replication across multiple nodes.
 */
public interface ReplicationManager {
    
    /**
     * Starts the replication manager.
     */
    void start();
    
    /**
     * Stops the replication manager.
     */
    void stop();
    
    /**
     * Replicates a PUT operation to all replica nodes.
     * 
     * @param key the key
     * @param value the value
     */
    void replicatePut(String key, String value);
    
    /**
     * Replicates a DELETE operation to all replica nodes.
     * 
     * @param key the key to delete
     */
    void replicateDelete(String key);
    
    /**
     * Adds a replica node to the cluster.
     * 
     * @param nodeInfo the node information
     */
    void addReplica(NodeInfo nodeInfo);
    
    /**
     * Removes a replica node from the cluster.
     * 
     * @param nodeId the node ID to remove
     */
    void removeReplica(String nodeId);
    
    /**
     * Gets all active replica nodes.
     * 
     * @return list of active replicas
     */
    List<NodeInfo> getActiveReplicas();
    
    /**
     * Checks if this node is the primary/leader.
     * 
     * @return true if this is the primary node
     */
    boolean isPrimary();
    
    /**
     * Triggers failover to a new primary node.
     */
    void triggerFailover();
    
    /**
     * Gets the current primary node.
     * 
     * @return the primary node info, or null if none
     */
    NodeInfo getPrimaryNode();
}

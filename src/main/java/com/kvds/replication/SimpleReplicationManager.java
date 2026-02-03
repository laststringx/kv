package com.kvds.replication;

import com.kvds.wal.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Simple implementation of ReplicationManager with basic replication and failover.
 * 
 * Features:
 * - Asynchronous replication to replica nodes
 * - Heartbeat-based failure detection
 * - Automatic failover to next available node
 * - Simple leader election (lowest node ID)
 */
public class SimpleReplicationManager implements ReplicationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleReplicationManager.class);
    
    private final NodeInfo localNode;
    private final Map<String, NodeInfo> replicas;
    private final ExecutorService replicationExecutor;
    private final ScheduledExecutorService heartbeatExecutor;
    private final ReplicationClient client;
    
    private volatile boolean running;
    private volatile NodeInfo primaryNode;
    
    // Configuration
    private static final long HEARTBEAT_INTERVAL_MS = 1000; // 1 second
    private static final long HEARTBEAT_TIMEOUT_MS = 5000; // 5 seconds
    private static final int REPLICATION_THREADS = 4;
    
    public SimpleReplicationManager(NodeInfo localNode) {
        this(localNode, new HttpReplicationClient());
    }
    
    public SimpleReplicationManager(NodeInfo localNode, ReplicationClient client) {
        this.localNode = localNode;
        this.client = client;
        this.replicas = new ConcurrentHashMap<>();
        this.replicationExecutor = Executors.newFixedThreadPool(REPLICATION_THREADS);
        this.heartbeatExecutor = Executors.newScheduledThreadPool(1);
        this.running = false;
        
        // Initially, this node is the primary if no others exist
        this.primaryNode = localNode;
    }
    
    @Override
    public void start() {
        if (running) {
            logger.warn("ReplicationManager already running");
            return;
        }
        
        running = true;
        logger.info("Starting ReplicationManager for node: {}", localNode.getNodeId());
        
        // Start heartbeat monitoring
        heartbeatExecutor.scheduleAtFixedRate(
            this::checkHeartbeats,
            HEARTBEAT_INTERVAL_MS,
            HEARTBEAT_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        // Send initial heartbeat to all replicas
        sendHeartbeats();
        
        logger.info("ReplicationManager started successfully");
    }
    
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        
        logger.info("Stopping ReplicationManager");
        running = false;
        
        heartbeatExecutor.shutdown();
        replicationExecutor.shutdown();
        
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
            if (!replicationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                replicationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            heartbeatExecutor.shutdownNow();
            replicationExecutor.shutdownNow();
        }
        
        logger.info("ReplicationManager stopped");
    }
    
    @Override
    public void replicatePut(String key, String value) {
        if (!isPrimary()) {
            logger.warn("Not primary node, skipping replication");
            return;
        }
        
        ReplicationMessage message = ReplicationMessage.builder()
            .type(ReplicationMessage.MessageType.REPLICATE_PUT)
            .sourceNodeId(localNode.getNodeId())
            .operation(Operation.PUT)
            .key(key)
            .value(value)
            .build();
        
        replicateToAll(message);
    }
    
    @Override
    public void replicateDelete(String key) {
        if (!isPrimary()) {
            logger.warn("Not primary node, skipping replication");
            return;
        }
        
        ReplicationMessage message = ReplicationMessage.builder()
            .type(ReplicationMessage.MessageType.REPLICATE_DELETE)
            .sourceNodeId(localNode.getNodeId())
            .operation(Operation.DELETE)
            .key(key)
            .build();
        
        replicateToAll(message);
    }
    
    private void replicateToAll(ReplicationMessage message) {
        List<NodeInfo> activeReplicas = getActiveReplicas();
        
        if (activeReplicas.isEmpty()) {
            logger.debug("No active replicas to replicate to");
            return;
        }
        
        logger.debug("Replicating {} to {} replicas", message.getType(), activeReplicas.size());
        
        // Asynchronous replication (fire and forget)
        for (NodeInfo replica : activeReplicas) {
            replicationExecutor.submit(() -> {
                try {
                    client.sendMessage(replica, message);
                    logger.trace("Replicated to node: {}", replica.getNodeId());
                } catch (Exception e) {
                    logger.error("Failed to replicate to node: {}", replica.getNodeId(), e);
                    // Mark node as potentially failed
                    replica.setStatus(NodeInfo.NodeStatus.INACTIVE);
                }
            });
        }
    }
    
    @Override
    public void addReplica(NodeInfo nodeInfo) {
        replicas.put(nodeInfo.getNodeId(), nodeInfo);
        logger.info("Added replica node: {}", nodeInfo);
        
        // Re-elect primary if needed
        electPrimary();
    }
    
    @Override
    public void removeReplica(String nodeId) {
        NodeInfo removed = replicas.remove(nodeId);
        if (removed != null) {
            logger.info("Removed replica node: {}", removed);
            
            // Re-elect primary if the removed node was primary
            if (primaryNode != null && primaryNode.getNodeId().equals(nodeId)) {
                electPrimary();
            }
        }
    }
    
    @Override
    public List<NodeInfo> getActiveReplicas() {
        return replicas.values().stream()
            .filter(node -> node.getStatus() == NodeInfo.NodeStatus.ACTIVE)
            .filter(node -> !node.getNodeId().equals(localNode.getNodeId()))
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean isPrimary() {
        return primaryNode != null && primaryNode.equals(localNode);
    }
    
    @Override
    public void triggerFailover() {
        logger.warn("Triggering manual failover");
        electPrimary();
    }
    
    @Override
    public NodeInfo getPrimaryNode() {
        return primaryNode;
    }
    
    /**
     * Sends heartbeats to all replica nodes.
     */
    private void sendHeartbeats() {
        ReplicationMessage heartbeat = ReplicationMessage.builder()
            .type(ReplicationMessage.MessageType.HEARTBEAT)
            .sourceNodeId(localNode.getNodeId())
            .build();
        
        for (NodeInfo replica : replicas.values()) {
            replicationExecutor.submit(() -> {
                try {
                    client.sendMessage(replica, heartbeat);
                    replica.updateHeartbeat();
                } catch (Exception e) {
                    logger.debug("Failed to send heartbeat to node: {}", replica.getNodeId());
                }
            });
        }
    }
    
    /**
     * Checks heartbeats and detects failed nodes.
     */
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        boolean failureDetected = false;
        
        for (NodeInfo replica : replicas.values()) {
            long timeSinceLastHeartbeat = now - replica.getLastHeartbeat();
            
            if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                if (replica.getStatus() == NodeInfo.NodeStatus.ACTIVE) {
                    logger.warn("Node {} failed (no heartbeat for {}ms)", 
                        replica.getNodeId(), timeSinceLastHeartbeat);
                    replica.setStatus(NodeInfo.NodeStatus.FAILED);
                    failureDetected = true;
                }
            }
        }
        
        // Send heartbeats
        sendHeartbeats();
        
        // Trigger failover if primary failed
        if (failureDetected && primaryNode != null) {
            NodeInfo primary = replicas.get(primaryNode.getNodeId());
            if (primary != null && primary.getStatus() == NodeInfo.NodeStatus.FAILED) {
                logger.error("Primary node {} failed, triggering failover", primaryNode.getNodeId());
                electPrimary();
            }
        }
    }
    
    /**
     * Elects a new primary node using simple algorithm (lowest node ID among active nodes).
     */
    private void electPrimary() {
        List<NodeInfo> allNodes = new ArrayList<>();
        allNodes.add(localNode);
        allNodes.addAll(replicas.values().stream()
            .filter(node -> node.getStatus() == NodeInfo.NodeStatus.ACTIVE)
            .collect(Collectors.toList()));
        
        if (allNodes.isEmpty()) {
            logger.error("No active nodes available for primary election");
            primaryNode = null;
            return;
        }
        
        // Simple election: lowest node ID wins
        NodeInfo newPrimary = allNodes.stream()
            .min(Comparator.comparing(NodeInfo::getNodeId))
            .orElse(localNode);
        
        if (!newPrimary.equals(primaryNode)) {
            NodeInfo oldPrimary = primaryNode;
            primaryNode = newPrimary;
            logger.info("Primary node changed: {} -> {}", 
                oldPrimary != null ? oldPrimary.getNodeId() : "none", 
                primaryNode.getNodeId());
            
            // Notify all nodes about failover
            if (isPrimary()) {
                notifyFailover();
            }
        }
    }
    
    /**
     * Notifies all replicas about a failover event.
     */
    private void notifyFailover() {
        ReplicationMessage message = ReplicationMessage.builder()
            .type(ReplicationMessage.MessageType.FAILOVER_NOTIFICATION)
            .sourceNodeId(localNode.getNodeId())
            .build();
        
        replicateToAll(message);
    }
    
    /**
     * Interface for sending replication messages to other nodes.
     */
    public interface ReplicationClient {
        void sendMessage(NodeInfo target, ReplicationMessage message) throws Exception;
    }
    
    /**
     * Simple HTTP-based replication client (stub implementation).
     */
    private static class HttpReplicationClient implements ReplicationClient {
        @Override
        public void sendMessage(NodeInfo target, ReplicationMessage message) throws Exception {
            // In a real implementation, this would use HTTP/gRPC to send the message
            // For now, this is a stub that simulates successful replication
            logger.trace("Simulating message send to {}: {}", target.getAddress(), message.getType());
        }
    }
}

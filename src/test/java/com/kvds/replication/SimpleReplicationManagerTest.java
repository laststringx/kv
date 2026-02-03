package com.kvds.replication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SimpleReplicationManager.
 */
class SimpleReplicationManagerTest {

    private SimpleReplicationManager replicationManager;
    private NodeInfo localNode;

    @BeforeEach
    void setUp() {
        localNode = new NodeInfo("node-1", "localhost", 8001);
        replicationManager = new SimpleReplicationManager(localNode);
    }

    @AfterEach
    void tearDown() {
        if (replicationManager != null) {
            replicationManager.stop();
        }
    }

    @Test
    void testStartAndStop() {
        replicationManager.start();
        assertTrue(replicationManager.isPrimary());
        
        replicationManager.stop();
    }

    @Test
    void testAddReplica() {
        replicationManager.start();
        
        NodeInfo replica = new NodeInfo("node-2", "localhost", 8002);
        replicationManager.addReplica(replica);
        
        List<NodeInfo> replicas = replicationManager.getActiveReplicas();
        assertEquals(1, replicas.size());
        assertEquals("node-2", replicas.get(0).getNodeId());
    }

    @Test
    void testRemoveReplica() {
        replicationManager.start();
        
        NodeInfo replica = new NodeInfo("node-2", "localhost", 8002);
        replicationManager.addReplica(replica);
        
        replicationManager.removeReplica("node-2");
        
        List<NodeInfo> replicas = replicationManager.getActiveReplicas();
        assertTrue(replicas.isEmpty());
    }

    @Test
    void testPrimaryElection() {
        replicationManager.start();
        
        // Initially, local node is primary
        assertTrue(replicationManager.isPrimary());
        assertEquals(localNode, replicationManager.getPrimaryNode());
        
        // Add a replica with lower ID (should become primary)
        NodeInfo replica = new NodeInfo("node-0", "localhost", 8000);
        replicationManager.addReplica(replica);
        
        // After election, node-0 should be primary (lowest ID)
        assertFalse(replicationManager.isPrimary());
        assertEquals("node-0", replicationManager.getPrimaryNode().getNodeId());
    }

    @Test
    void testReplicatePut() {
        replicationManager.start();
        
        NodeInfo replica1 = new NodeInfo("node-2", "localhost", 8002);
        NodeInfo replica2 = new NodeInfo("node-3", "localhost", 8003);
        
        replicationManager.addReplica(replica1);
        replicationManager.addReplica(replica2);
        
        // Should not throw exception
        replicationManager.replicatePut("key1", "value1");
    }

    @Test
    void testReplicateDelete() {
        replicationManager.start();
        
        NodeInfo replica = new NodeInfo("node-2", "localhost", 8002);
        replicationManager.addReplica(replica);
        
        // Should not throw exception
        replicationManager.replicateDelete("key1");
    }

    @Test
    void testFailover() {
        replicationManager.start();
        
        NodeInfo replica = new NodeInfo("node-2", "localhost", 8002);
        replicationManager.addReplica(replica);
        
        // Trigger manual failover
        replicationManager.triggerFailover();
        
        // Should still have a primary
        assertNotNull(replicationManager.getPrimaryNode());
    }

    @Test
    void testMultipleReplicas() {
        replicationManager.start();
        
        for (int i = 2; i <= 5; i++) {
            NodeInfo replica = new NodeInfo("node-" + i, "localhost", 8000 + i);
            replicationManager.addReplica(replica);
        }
        
        List<NodeInfo> replicas = replicationManager.getActiveReplicas();
        assertEquals(4, replicas.size());
    }

    @Test
    void testNodeInfo() {
        NodeInfo node = new NodeInfo("test-node", "192.168.1.1", 9000);
        
        assertEquals("test-node", node.getNodeId());
        assertEquals("192.168.1.1", node.getHost());
        assertEquals(9000, node.getPort());
        assertEquals("192.168.1.1:9000", node.getAddress());
        assertEquals(NodeInfo.NodeStatus.ACTIVE, node.getStatus());
        
        node.setStatus(NodeInfo.NodeStatus.FAILED);
        assertEquals(NodeInfo.NodeStatus.FAILED, node.getStatus());
        
        long before = node.getLastHeartbeat();
        node.updateHeartbeat();
        long after = node.getLastHeartbeat();
        assertTrue(after >= before);
    }
}

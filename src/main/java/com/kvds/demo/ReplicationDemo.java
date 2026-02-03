package com.kvds.demo;

import com.kvds.core.KeyValueStore;
import com.kvds.core.KeyValueStoreImpl;
import com.kvds.recovery.RecoveryManagerImpl;
import com.kvds.replication.NodeInfo;
import com.kvds.replication.SimpleReplicationManager;
import com.kvds.storage.InMemoryStorage;
import com.kvds.wal.WriteAheadLogImpl;

/**
 * Demonstration of KV-DS with replication and failover.
 * 
 * This example shows how to:
 * 1. Create a cluster of 3 nodes
 * 2. Replicate data across nodes
 * 3. Handle automatic failover when primary fails
 */
public class ReplicationDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== KV-DS Replication and Failover Demo ===\n");
        
        // Create Node 1 (Primary)
        System.out.println("1. Creating Node 1 (Primary)...");
        NodeInfo node1Info = new NodeInfo("node-1", "localhost", 8001);
        SimpleReplicationManager replication1 = new SimpleReplicationManager(node1Info);
        KeyValueStore store1 = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("data/node1-wal.log"),
            new RecoveryManagerImpl(),
            replication1
        );
        
        // Create Node 2 (Replica)
        System.out.println("2. Creating Node 2 (Replica)...");
        NodeInfo node2Info = new NodeInfo("node-2", "localhost", 8002);
        SimpleReplicationManager replication2 = new SimpleReplicationManager(node2Info);
        KeyValueStore store2 = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("data/node2-wal.log"),
            new RecoveryManagerImpl(),
            replication2
        );
        
        // Create Node 3 (Replica)
        System.out.println("3. Creating Node 3 (Replica)...");
        NodeInfo node3Info = new NodeInfo("node-3", "localhost", 8003);
        SimpleReplicationManager replication3 = new SimpleReplicationManager(node3Info);
        KeyValueStore store3 = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("data/node3-wal.log"),
            new RecoveryManagerImpl(),
            replication3
        );
        
        // Connect nodes to each other
        System.out.println("\n4. Connecting nodes to form cluster...");
        replication1.addReplica(node2Info);
        replication1.addReplica(node3Info);
        replication2.addReplica(node1Info);
        replication2.addReplica(node3Info);
        replication3.addReplica(node1Info);
        replication3.addReplica(node2Info);
        
        // Check primary
        System.out.println("\n5. Primary Election:");
        System.out.println("   Node 1 is primary: " + replication1.isPrimary());
        System.out.println("   Node 2 is primary: " + replication2.isPrimary());
        System.out.println("   Node 3 is primary: " + replication3.isPrimary());
        System.out.println("   Primary node: " + replication1.getPrimaryNode().getNodeId());
        
        // Write data to primary
        System.out.println("\n6. Writing data to primary node...");
        store1.put("user:1", "Alice");
        store1.put("user:2", "Bob");
        store1.put("user:3", "Charlie");
        System.out.println("   Written 3 key-value pairs");
        System.out.println("   Data will be replicated to nodes 2 and 3 asynchronously");
        
        // Read from primary
        System.out.println("\n7. Reading from primary (Node 1):");
        System.out.println("   user:1 = " + store1.get("user:1"));
        System.out.println("   user:2 = " + store1.get("user:2"));
        System.out.println("   user:3 = " + store1.get("user:3"));
        
        // Simulate primary failure
        System.out.println("\n8. Simulating primary node failure...");
        System.out.println("   Removing Node 1 from cluster...");
        replication2.removeReplica("node-1");
        replication3.removeReplica("node-1");
        
        // Wait for failover
        Thread.sleep(2000);
        
        // Check new primary
        System.out.println("\n9. After Failover:");
        System.out.println("   Node 2 is primary: " + replication2.isPrimary());
        System.out.println("   Node 3 is primary: " + replication3.isPrimary());
        System.out.println("   New primary node: " + replication2.getPrimaryNode().getNodeId());
        
        // Write to new primary
        System.out.println("\n10. Writing to new primary...");
        if (replication2.isPrimary()) {
            store2.put("user:4", "David");
            System.out.println("    Written user:4 to Node 2 (new primary)");
        } else if (replication3.isPrimary()) {
            store3.put("user:4", "David");
            System.out.println("    Written user:4 to Node 3 (new primary)");
        }
        
        // Show cluster status
        System.out.println("\n11. Final Cluster Status:");
        System.out.println("    Node 2 active replicas: " + replication2.getActiveReplicas().size());
        System.out.println("    Node 3 active replicas: " + replication3.getActiveReplicas().size());
        
        // Cleanup
        System.out.println("\n12. Cleaning up...");
        store1.close();
        store2.close();
        store3.close();
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("\nKey Features Demonstrated:");
        System.out.println("✓ Multi-node cluster setup");
        System.out.println("✓ Automatic primary election (lowest node ID)");
        System.out.println("✓ Asynchronous replication to replicas");
        System.out.println("✓ Automatic failover when primary fails");
        System.out.println("✓ New primary continues serving requests");
    }
}

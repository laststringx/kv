# Replication and Failover Guide

## Overview

KV-DS now supports **data replication** across multiple nodes and **automatic failover** when the primary node fails. This provides high availability and fault tolerance for production deployments.

## Architecture

### Cluster Components

1. **Primary Node**: Accepts write operations and replicates to replicas
2. **Replica Nodes**: Receive replicated data from primary
3. **Replication Manager**: Handles replication, heartbeats, and failover

### How It Works

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ write
       ▼
┌─────────────┐      replicate      ┌─────────────┐
│  Primary    │ ─────────────────▶  │  Replica 1  │
│  (Node 1)   │                     │  (Node 2)   │
└─────────────┘                     └─────────────┘
       │                                    
       │ replicate                          
       ▼                                    
┌─────────────┐                            
│  Replica 2  │                            
│  (Node 3)   │                            
└─────────────┘                            
```

## Features

✅ **Asynchronous Replication**: Non-blocking writes with async replication  
✅ **Heartbeat Monitoring**: Detects node failures automatically  
✅ **Automatic Failover**: Promotes replica to primary when primary fails  
✅ **Leader Election**: Simple algorithm (lowest node ID wins)  
✅ **Multi-Node Support**: Support for N replicas  

## Quick Start

### 1. Create a Cluster

```java
import com.kvds.core.*;
import com.kvds.replication.*;
import com.kvds.storage.*;
import com.kvds.wal.*;
import com.kvds.recovery.*;

// Node 1 (Primary)
NodeInfo node1 = new NodeInfo("node-1", "localhost", 8001);
SimpleReplicationManager replication1 = new SimpleReplicationManager(node1);
KeyValueStore store1 = new KeyValueStoreImpl(
    new InMemoryStorage(),
    new WriteAheadLogImpl("data/node1.log"),
    new RecoveryManagerImpl(),
    replication1
);

// Node 2 (Replica)
NodeInfo node2 = new NodeInfo("node-2", "localhost", 8002);
SimpleReplicationManager replication2 = new SimpleReplicationManager(node2);
KeyValueStore store2 = new KeyValueStoreImpl(
    new InMemoryStorage(),
    new WriteAheadLogImpl("data/node2.log"),
    new RecoveryManagerImpl(),
    replication2
);

// Connect nodes
replication1.addReplica(node2);
replication2.addReplica(node1);
```

### 2. Write Data (Primary Only)

```java
// Check if this node is primary
if (replication1.isPrimary()) {
    store1.put("key1", "value1");  // Automatically replicated to node2
    store1.put("key2", "value2");
}
```

### 3. Handle Failover

When the primary fails, a replica automatically becomes the new primary:

```java
// Simulate primary failure
replication2.removeReplica("node-1");

// Wait for failover (automatic)
Thread.sleep(2000);

// Check new primary
System.out.println("New primary: " + replication2.getPrimaryNode().getNodeId());

// Continue writing to new primary
if (replication2.isPrimary()) {
    store2.put("key3", "value3");
}
```

## Configuration

### Heartbeat Settings

Edit `SimpleReplicationManager.java`:

```java
private static final long HEARTBEAT_INTERVAL_MS = 1000;  // 1 second
private static final long HEARTBEAT_TIMEOUT_MS = 5000;   // 5 seconds
```

### Replication Threads

```java
private static final int REPLICATION_THREADS = 4;  // Concurrent replication workers
```

## API Reference

### ReplicationManager Interface

#### `void start()`
Starts the replication manager and heartbeat monitoring.

#### `void stop()`
Stops the replication manager gracefully.

#### `void replicatePut(String key, String value)`
Replicates a PUT operation to all active replicas (async).

#### `void replicateDelete(String key)`
Replicates a DELETE operation to all active replicas (async).

#### `void addReplica(NodeInfo nodeInfo)`
Adds a new replica node to the cluster.

#### `void removeReplica(String nodeId)`
Removes a replica node from the cluster.

#### `boolean isPrimary()`
Returns true if this node is the current primary.

#### `void triggerFailover()`
Manually triggers a failover/re-election.

#### `NodeInfo getPrimaryNode()`
Returns the current primary node information.

#### `List<NodeInfo> getActiveReplicas()`
Returns list of all active replica nodes.

## Failure Detection

The system uses heartbeat-based failure detection:

1. Every node sends heartbeats to all other nodes every 1 second
2. If a node doesn't respond for 5 seconds, it's marked as FAILED
3. If the primary fails, automatic failover is triggered
4. The replica with the lowest node ID becomes the new primary

## Limitations

⚠️ **Current Implementation Limitations:**

1. **Stub Replication**: The actual network communication is stubbed out
   - Real implementation would use HTTP/gRPC
   - Currently simulates successful replication
   
2. **Simple Leader Election**: Uses lowest node ID
   - Production systems should use Raft or Paxos
   
3. **No Split-Brain Protection**: No quorum-based decisions
   - Could lead to split-brain in network partitions
   
4. **Fire-and-Forget Replication**: No acknowledgment tracking
   - Production systems should wait for replica ACKs

5. **No Data Synchronization**: New replicas don't sync existing data
   - Would need full sync or snapshot transfer

## Production Considerations

For production use, you should implement:

1. **Real Network Communication**: Replace stub with HTTP/gRPC
2. **Consensus Protocol**: Use Raft or Paxos for leader election
3. **Quorum Writes**: Wait for majority of replicas before ACK
4. **Data Sync**: Implement snapshot transfer for new replicas
5. **Monitoring**: Add metrics for replication lag, failures
6. **Configuration**: Externalize cluster configuration

## Demo

Run the replication demo:

```bash
mvn exec:java -Dexec.mainClass="com.kvds.demo.ReplicationDemo"
```

This demonstrates:
- Creating a 3-node cluster
- Writing to primary with replication
- Simulating primary failure
- Automatic failover to new primary

## Testing

Run replication tests:

```bash
mvn test -Dtest=SimpleReplicationManagerTest
```

## Troubleshooting

### Replica not receiving updates
- Check that nodes are properly connected with `addReplica()`
- Verify the writing node is the primary with `isPrimary()`
- Check logs for replication errors

### Failover not happening
- Ensure heartbeat timeout is configured correctly
- Check that failed node was actually removed from cluster
- Verify at least one replica is still active

### Multiple primaries
- This indicates a split-brain scenario
- Restart all nodes and re-establish cluster
- Consider implementing quorum-based decisions

## Next Steps

To make this production-ready:

1. Implement real network communication (HTTP/gRPC)
2. Add Raft consensus protocol
3. Implement quorum-based writes
4. Add data synchronization for new replicas
5. Add comprehensive monitoring and metrics
6. Implement configuration management
7. Add security (TLS, authentication)

---

**Status**: Basic implementation complete ✅  
**Production Ready**: No (requires network layer and consensus)  
**Suitable For**: Demonstration and learning purposes

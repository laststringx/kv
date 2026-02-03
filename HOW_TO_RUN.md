# How to Run KV-DS

Quick start guide for running the KV-DS distributed key-value data store.

## Prerequisites

### Required
- **Java 17** or higher
- **Maven 3.6** or higher

### Verify Installation

```bash
# Check Java version
java -version
# Should show: openjdk version "17.x.x" or higher

# Check Maven version
mvn -version
# Should show: Apache Maven 3.6.x or higher
```

## Quick Start (5 Minutes)

### 1. Clone the Repository

```bash
git clone https://github.com/laststringx/kv.git
cd kv
```

### 2. Build the Project

```bash
mvn clean compile
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~5 seconds
```

### 3. Run Tests

```bash
mvn clean test
```

Expected output:
```
[INFO] Tests run: 100+, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 4. Run Demo

```bash
# Quick demo (single node)
mvn exec:java -Dexec.mainClass="com.kvds.demo.QuickDemo"

# Replication demo (3-node cluster)
mvn exec:java -Dexec.mainClass="com.kvds.demo.ReplicationDemo"
```

## Usage Modes

### Mode 1: Simple In-Memory Store (No Persistence)

**Use Case**: Testing, development, temporary data

```java
import com.kvds.core.*;
import com.kvds.storage.*;

public class SimpleExample {
    public static void main(String[] args) {
        // Create store
        KeyValueStore store = new KeyValueStoreImpl(new InMemoryStorage());
        
        // Use it
        store.put("key1", "value1");
        System.out.println(store.get("key1"));  // Prints: value1
        
        // Cleanup
        store.close();
    }
}
```

**Characteristics:**
- ✅ Fastest (no disk I/O)
- ❌ No durability (data lost on crash)
- ✅ Thread-safe

### Mode 2: Persistent Store with WAL (Single Node)

**Use Case**: Production single-node deployment

```java
import com.kvds.core.*;
import com.kvds.storage.*;
import com.kvds.wal.*;
import com.kvds.recovery.*;

public class PersistentExample {
    public static void main(String[] args) {
        // Create store with WAL
        KeyValueStore store = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("./data/wal.log"),
            new RecoveryManagerImpl()
        );
        
        // All operations are durable
        store.put("key1", "value1");
        
        // Data survives crashes
        // On restart, recovery happens automatically
        
        store.close();
    }
}
```

**Characteristics:**
- ✅ Durable (survives crashes)
- ✅ Automatic recovery
- ⚠️ Slower writes (~100-1000μs per write)
- ✅ Thread-safe

### Mode 3: High-Performance Store (Async WAL)

**Use Case**: High-throughput scenarios

```java
import com.kvds.wal.*;

public class HighPerformanceExample {
    public static void main(String[] args) {
        // Create async WAL
        WriteAheadLog syncWal = new WriteAheadLogImpl("./data/wal.log");
        AsyncWriteAheadLog asyncWal = new AsyncWriteAheadLog(syncWal);
        
        KeyValueStore store = new KeyValueStoreImpl(
            new InMemoryStorage(),
            asyncWal,
            new RecoveryManagerImpl()
        );
        
        // High-throughput writes (batched)
        for (int i = 0; i < 100000; i++) {
            store.put("key-" + i, "value-" + i);
        }
        
        store.close();  // Flushes all pending writes
    }
}
```

**Characteristics:**
- ✅ Very fast writes (~10-50μs per write)
- ✅ Durable (batched to disk)
- ✅ High throughput (50K-100K ops/sec)
- ✅ Thread-safe

### Mode 4: Distributed Cluster (3 Nodes)

**Use Case**: High availability, fault tolerance

```java
import com.kvds.replication.*;

public class ClusterExample {
    public static void main(String[] args) throws InterruptedException {
        // Create 3 nodes
        NodeInfo node1 = new NodeInfo("node-1", "localhost", 8001);
        NodeInfo node2 = new NodeInfo("node-2", "localhost", 8002);
        NodeInfo node3 = new NodeInfo("node-3", "localhost", 8003);
        
        // Setup replication managers
        SimpleReplicationManager rep1 = new SimpleReplicationManager(node1);
        SimpleReplicationManager rep2 = new SimpleReplicationManager(node2);
        SimpleReplicationManager rep3 = new SimpleReplicationManager(node3);
        
        // Create stores
        KeyValueStore store1 = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("data/node1.log"),
            new RecoveryManagerImpl(),
            rep1
        );
        
        KeyValueStore store2 = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("data/node2.log"),
            new RecoveryManagerImpl(),
            rep2
        );
        
        KeyValueStore store3 = new KeyValueStoreImpl(
            new InMemoryStorage(),
            new WriteAheadLogImpl("data/node3.log"),
            new RecoveryManagerImpl(),
            rep3
        );
        
        // Connect nodes
        rep1.addReplica(node2);
        rep1.addReplica(node3);
        rep2.addReplica(node1);
        rep2.addReplica(node3);
        rep3.addReplica(node1);
        rep3.addReplica(node2);
        
        // Write to primary
        if (rep1.isPrimary()) {
            store1.put("key1", "value1");  // Replicated to all nodes
        }
        
        // Simulate primary failure
        rep2.removeReplica("node-1");
        rep3.removeReplica("node-1");
        Thread.sleep(2000);  // Wait for failover
        
        // New primary continues
        if (rep2.isPrimary()) {
            store2.put("key2", "value2");
        }
        
        // Cleanup
        store1.close();
        store2.close();
        store3.close();
    }
}
```

**Characteristics:**
- ✅ High availability
- ✅ Automatic failover
- ✅ Data replication
- ✅ Fault tolerant
- ⚠️ Network layer is stubbed (demo only)

## Running Tests

### All Tests

```bash
mvn clean test
```

### Specific Test Suites

```bash
# Core functionality
mvn test -Dtest=KeyValueStoreImplTest

# Integration tests
mvn test -Dtest=KeyValueStoreIntegrationTest

# Recovery tests
mvn test -Dtest=RecoveryManagerImplTest

# Replication tests
mvn test -Dtest=SimpleReplicationManagerTest

# WAL tests
mvn test -Dtest=WriteAheadLogImplTest
mvn test -Dtest=AsyncWriteAheadLogTest
```

### Test Coverage Report

```bash
mvn clean test jacoco:report
```

View report: Open `target/site/jacoco/index.html` in browser

## Building JAR

### Create Executable JAR

```bash
mvn clean package
```

Output: `target/kv-datastore-1.0.0.jar`

### Run JAR

```bash
java -jar target/kv-datastore-1.0.0.jar
```

## Command-Line Interface (CLI)

### Windows

```bash
kvds.bat
```

### Linux/Mac

```bash
./kvds.sh
```

### CLI Commands

```
PUT <key> <value>     Store a key-value pair
GET <key>             Retrieve value for a key
DELETE <key>          Delete a key
CLEAR                 Clear all data
STATUS                Show store status
HELP                  Show help
EXIT                  Exit CLI
```

### CLI Example Session

```
KV-DS> PUT user:1 Alice
OK

KV-DS> PUT user:2 Bob
OK

KV-DS> GET user:1
Alice

KV-DS> DELETE user:1
OK

KV-DS> STATUS
Total keys: 1

KV-DS> EXIT
Goodbye!
```

## Configuration

### WAL Configuration

Create `config.properties`:

```properties
# WAL settings
wal.enabled=true
wal.path=./data/wal.log
wal.async=true
wal.batch.size=100
wal.batch.timeout.ms=10

# Replication settings
replication.enabled=true
replication.heartbeat.interval.ms=1000
replication.heartbeat.timeout.ms=5000
```

### JVM Options

For better performance:

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -jar kv-datastore.jar
```

Options explained:
- `-Xms512m`: Initial heap size
- `-Xmx2g`: Maximum heap size
- `-XX:+UseG1GC`: Use G1 garbage collector

## Troubleshooting

### Issue: "JAVA_HOME not set"

**Solution:**
```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17

# Linux/Mac
export JAVA_HOME=/usr/lib/jvm/java-17
```

### Issue: "Tests failing"

**Solution:**
```bash
# Clean and rebuild
mvn clean compile test

# Skip tests temporarily
mvn clean package -DskipTests
```

### Issue: "Port already in use" (Replication demo)

**Solution:**
- Change port numbers in demo code
- Or kill process using the port:
  ```bash
  # Windows
  netstat -ano | findstr :8001
  taskkill /PID <PID> /F
  
  # Linux/Mac
  lsof -i :8001
  kill -9 <PID>
  ```

### Issue: "Out of memory"

**Solution:**
```bash
# Increase heap size
mvn -DargLine="-Xmx4g" test

# Or for running application
java -Xmx4g -jar kv-datastore.jar
```

## Performance Tuning

### For High Throughput

1. **Use Async WAL**:
   ```java
   AsyncWriteAheadLog asyncWal = new AsyncWriteAheadLog(
       syncWal,
       10000,  // Large queue
       100,    // Batch size
       10      // Low timeout
   );
   ```

2. **Increase JVM Heap**:
   ```bash
   java -Xms2g -Xmx4g ...
   ```

3. **Use G1GC**:
   ```bash
   java -XX:+UseG1GC ...
   ```

### For Low Latency

1. **Use Sync WAL** (if durability needed)
2. **Or disable WAL** (if durability not needed)
3. **Reduce batch timeout** in async mode

### For Large Datasets

1. **Increase heap size**:
   ```bash
   java -Xmx8g ...
   ```

2. **Monitor memory usage**:
   ```bash
   jstat -gc <PID> 1000
   ```

## Next Steps

1. **Read the README**: Full documentation in `README.md`
2. **Run the demos**: Try `QuickDemo` and `ReplicationDemo`
3. **Explore the code**: Check `src/main/java/com/kvds/`
4. **Run tests**: See `src/test/java/com/kvds/`
5. **Integrate**: Use KV-DS in your application

## Support

- **GitHub**: https://github.com/laststringx/kv
- **Issues**: https://github.com/laststringx/kv/issues
- **Documentation**: See `README.md`

## Quick Reference

| Command | Description |
|---------|-------------|
| `mvn clean compile` | Build project |
| `mvn test` | Run all tests |
| `mvn package` | Create JAR |
| `mvn exec:java -Dexec.mainClass="..."` | Run demo |
| `kvds.bat` / `./kvds.sh` | Start CLI |

---

**Happy coding!** 🚀

# KV-DS High-Level Design

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATION                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ put(key, value)
                                │ get(key)
                                │ delete(key)
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      KeyValueStoreImpl                           │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  - Coordinates operations between Storage and WAL       │    │
│  │  - Ensures atomicity: WAL first, then Storage          │    │
│  │  - Delegates recovery to RecoveryManager               │    │
│  └────────────────────────────────────────────────────────┘    │
└──────────────────┬──────────────────────────┬───────────────────┘
                   │                          │
                   │                          │
         ┌─────────▼─────────┐      ┌────────▼──────────┐
         │  WriteAheadLog    │      │     Storage       │
         │   (Interface)     │      │   (Interface)     │
         └─────────┬─────────┘      └────────┬──────────┘
                   │                          │
                   │                          │
         ┌─────────▼─────────┐      ┌────────▼──────────┐
         │ WriteAheadLogImpl │      │  InMemoryStorage  │
         │                   │      │                   │
         │ - append()        │      │ - ConcurrentHashMap│
         │ - readAll()       │      │ - put()           │
         │ - clear()         │      │ - get()           │
         │                   │      │ - delete()        │
         └─────────┬─────────┘      └───────────────────┘
                   │
                   │
         ┌─────────▼─────────┐
         │   wal.log file    │
         │  (Persistent)     │
         └───────────────────┘
```

---

## Component Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         CORE LAYER                                │
│  ┌────────────────────────────────────────────────────────┐      │
│  │              KeyValueStore (Interface)                  │      │
│  │  + put(key: String, value: String): void              │      │
│  │  + get(key: String): String                            │      │
│  │  + delete(key: String): void                           │      │
│  │  + clear(): void                                       │      │
│  └────────────────────────────────────────────────────────┘      │
│                              △                                    │
│                              │                                    │
│                              │ implements                         │
│                              │                                    │
│  ┌────────────────────────────────────────────────────────┐      │
│  │           KeyValueStoreImpl (Class)                     │      │
│  │  - storage: Storage                                    │      │
│  │  - wal: WriteAheadLog                                  │      │
│  │  - recoveryManager: RecoveryManager                    │      │
│  │                                                         │      │
│  │  + KeyValueStoreImpl(walPath: String)                  │      │
│  │  + put(key, value): void                               │      │
│  │  + get(key): String                                    │      │
│  │  + delete(key): void                                   │      │
│  └────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                       STORAGE LAYER                               │
│  ┌────────────────────────────────────────────────────────┐      │
│  │                Storage (Interface)                      │      │
│  │  + put(key: String, value: String): void              │      │
│  │  + get(key: String): String                            │      │
│  │  + delete(key: String): void                           │      │
│  │  + clear(): void                                       │      │
│  │  + containsKey(key: String): boolean                   │      │
│  └────────────────────────────────────────────────────────┘      │
│                              △                                    │
│                              │ implements                         │
│  ┌────────────────────────────────────────────────────────┐      │
│  │           InMemoryStorage (Class)                       │      │
│  │  - store: ConcurrentHashMap<String, String>            │      │
│  │                                                         │      │
│  │  + put(key, value): void                               │      │
│  │  + get(key): String                                    │      │
│  │  + delete(key): void                                   │      │
│  │  + clear(): void                                       │      │
│  │  + containsKey(key): boolean                           │      │
│  └────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                         WAL LAYER                                 │
│  ┌────────────────────────────────────────────────────────┐      │
│  │            WriteAheadLog (Interface)                    │      │
│  │  + append(entry: LogEntry): void                       │      │
│  │  + readAll(): List<LogEntry>                           │      │
│  │  + clear(): void                                       │      │
│  └────────────────────────────────────────────────────────┘      │
│                              △                                    │
│                              │ implements                         │
│  ┌────────────────────────────────────────────────────────┐      │
│  │          WriteAheadLogImpl (Class)                      │      │
│  │  - logFilePath: String                                 │      │
│  │  - writer: BufferedWriter                              │      │
│  │                                                         │      │
│  │  + append(entry: LogEntry): void                       │      │
│  │  + readAll(): List<LogEntry>                           │      │
│  │  + clear(): void                                       │      │
│  │  - fsync(): void                                       │      │
│  └────────────────────────────────────────────────────────┘      │
│                                                                    │
│  ┌────────────────────────────────────────────────────────┐      │
│  │              LogEntry (Class)                           │      │
│  │  - timestamp: long                                     │      │
│  │  - operation: Operation                                │      │
│  │  - key: String                                         │      │
│  │  - value: String                                       │      │
│  │                                                         │      │
│  │  + serialize(): String                                 │      │
│  │  + deserialize(line: String): LogEntry                 │      │
│  └────────────────────────────────────────────────────────┘      │
│                                                                    │
│  ┌────────────────────────────────────────────────────────┐      │
│  │              Operation (Enum)                           │      │
│  │  - PUT                                                 │      │
│  │  - DELETE                                              │      │
│  └────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                      RECOVERY LAYER                               │
│  ┌────────────────────────────────────────────────────────┐      │
│  │           RecoveryManager (Interface)                   │      │
│  │  + recover(wal: WriteAheadLog, storage: Storage): void │      │
│  └────────────────────────────────────────────────────────┘      │
│                              △                                    │
│                              │ implements                         │
│  ┌────────────────────────────────────────────────────────┐      │
│  │         RecoveryManagerImpl (Class)                     │      │
│  │                                                         │      │
│  │  + recover(wal, storage): void                         │      │
│  │  - replayLogEntry(entry, storage): void                │      │
│  └────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                     EXCEPTION LAYER                               │
│  ┌────────────────────────────────────────────────────────┐      │
│  │            KVDSException (Class)                        │      │
│  │  extends RuntimeException                              │      │
│  └────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────┘
```

---

## Sequence Diagram: PUT Operation

```
Client          KeyValueStore      WriteAheadLog       Storage
  │                   │                   │               │
  │  put(k, v)        │                   │               │
  │──────────────────>│                   │               │
  │                   │                   │               │
  │                   │  append(entry)    │               │
  │                   │──────────────────>│               │
  │                   │                   │               │
  │                   │                   │ write to file │
  │                   │                   │───────┐       │
  │                   │                   │       │       │
  │                   │                   │<──────┘       │
  │                   │                   │               │
  │                   │                   │ fsync()       │
  │                   │                   │───────┐       │
  │                   │                   │       │       │
  │                   │                   │<──────┘       │
  │                   │                   │               │
  │                   │      success      │               │
  │                   │<──────────────────│               │
  │                   │                   │               │
  │                   │  put(k, v)        │               │
  │                   │───────────────────────────────────>│
  │                   │                   │               │
  │                   │                   │  store in map │
  │                   │                   │               │───┐
  │                   │                   │               │   │
  │                   │                   │               │<──┘
  │                   │                   │               │
  │                   │             success               │
  │                   │<───────────────────────────────────│
  │                   │                   │               │
  │      success      │                   │               │
  │<──────────────────│                   │               │
  │                   │                   │               │
```

---

## Sequence Diagram: GET Operation

```
Client          KeyValueStore       Storage
  │                   │                │
  │  get(k)           │                │
  │──────────────────>│                │
  │                   │                │
  │                   │  get(k)        │
  │                   │───────────────>│
  │                   │                │
  │                   │                │ lookup in map
  │                   │                │────┐
  │                   │                │    │
  │                   │                │<───┘
  │                   │                │
  │                   │    value       │
  │                   │<───────────────│
  │                   │                │
  │      value        │                │
  │<──────────────────│                │
  │                   │                │
```

---

## Sequence Diagram: DELETE Operation

```
Client          KeyValueStore      WriteAheadLog       Storage
  │                   │                   │               │
  │  delete(k)        │                   │               │
  │──────────────────>│                   │               │
  │                   │                   │               │
  │                   │  append(entry)    │               │
  │                   │──────────────────>│               │
  │                   │                   │               │
  │                   │                   │ write to file │
  │                   │                   │───────┐       │
  │                   │                   │       │       │
  │                   │                   │<──────┘       │
  │                   │                   │               │
  │                   │                   │ fsync()       │
  │                   │                   │───────┐       │
  │                   │                   │       │       │
  │                   │                   │<──────┘       │
  │                   │                   │               │
  │                   │      success      │               │
  │                   │<──────────────────│               │
  │                   │                   │               │
  │                   │  delete(k)        │               │
  │                   │───────────────────────────────────>│
  │                   │                   │               │
  │                   │                   │ remove from map│
  │                   │                   │               │───┐
  │                   │                   │               │   │
  │                   │                   │               │<──┘
  │                   │                   │               │
  │                   │             success               │
  │                   │<───────────────────────────────────│
  │                   │                   │               │
  │      success      │                   │               │
  │<──────────────────│                   │               │
  │                   │                   │               │
```

---

## Sequence Diagram: Recovery on Startup

```
Client      KeyValueStore    RecoveryManager   WriteAheadLog    Storage
  │               │                 │                │             │
  │  new KVStore()│                 │                │             │
  │──────────────>│                 │                │             │
  │               │                 │                │             │
  │               │ recover()       │                │             │
  │               │────────────────>│                │             │
  │               │                 │                │             │
  │               │                 │  readAll()     │             │
  │               │                 │───────────────>│             │
  │               │                 │                │             │
  │               │                 │                │ read file   │
  │               │                 │                │─────┐       │
  │               │                 │                │     │       │
  │               │                 │                │<────┘       │
  │               │                 │                │             │
  │               │                 │  List<Entry>   │             │
  │               │                 │<───────────────│             │
  │               │                 │                │             │
  │               │                 │  for each entry             │
  │               │                 │────────────┐   │             │
  │               │                 │            │   │             │
  │               │                 │  put/delete│   │             │
  │               │                 │────────────────────────────>│
  │               │                 │            │   │             │
  │               │                 │            │   │  apply op   │
  │               │                 │            │   │             │──┐
  │               │                 │            │   │             │  │
  │               │                 │            │   │             │<─┘
  │               │                 │            │   │             │
  │               │                 │<────────────────────────────│
  │               │                 │<───────────┘   │             │
  │               │                 │                │             │
  │               │    success      │                │             │
  │               │<────────────────│                │             │
  │               │                 │                │             │
  │    KVStore    │                 │                │             │
  │<──────────────│                 │                │             │
  │               │                 │                │             │
```

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    WRITE PATH (PUT/DELETE)                   │
└─────────────────────────────────────────────────────────────┘

    ┌──────────┐
    │  Client  │
    └────┬─────┘
         │
         │ 1. put(key, value)
         │
         ▼
    ┌────────────────┐
    │ KeyValueStore  │
    └────┬───────────┘
         │
         │ 2. Create LogEntry
         │
         ▼
    ┌────────────────┐
    │ WriteAheadLog  │
    └────┬───────────┘
         │
         │ 3. Serialize & Write
         │
         ▼
    ┌────────────────┐
    │   wal.log      │◄───── PERSISTENT STORAGE
    │   (File)       │
    └────────────────┘
         │
         │ 4. fsync() - Ensure durability
         │
         ▼
    ┌────────────────┐
    │ KeyValueStore  │
    └────┬───────────┘
         │
         │ 5. Update in-memory
         │
         ▼
    ┌────────────────┐
    │    Storage     │
    │ (ConcurrentMap)│◄───── IN-MEMORY STORAGE
    └────────────────┘


┌─────────────────────────────────────────────────────────────┐
│                       READ PATH (GET)                        │
└─────────────────────────────────────────────────────────────┘

    ┌──────────┐
    │  Client  │
    └────┬─────┘
         │
         │ 1. get(key)
         │
         ▼
    ┌────────────────┐
    │ KeyValueStore  │
    └────┬───────────┘
         │
         │ 2. Lookup
         │
         ▼
    ┌────────────────┐
    │    Storage     │
    │ (ConcurrentMap)│◄───── IN-MEMORY STORAGE
    └────┬───────────┘
         │
         │ 3. Return value
         │
         ▼
    ┌────────────────┐
    │ KeyValueStore  │
    └────┬───────────┘
         │
         │ 4. Return to client
         │
         ▼
    ┌──────────┐
    │  Client  │
    └──────────┘


┌─────────────────────────────────────────────────────────────┐
│                    RECOVERY PATH (STARTUP)                   │
└─────────────────────────────────────────────────────────────┘

    ┌──────────┐
    │  Client  │
    └────┬─────┘
         │
         │ 1. new KeyValueStore()
         │
         ▼
    ┌────────────────┐
    │ KeyValueStore  │
    └────┬───────────┘
         │
         │ 2. Trigger recovery
         │
         ▼
    ┌────────────────┐
    │RecoveryManager │
    └────┬───────────┘
         │
         │ 3. Read all entries
         │
         ▼
    ┌────────────────┐
    │   wal.log      │◄───── PERSISTENT STORAGE
    │   (File)       │
    └────┬───────────┘
         │
         │ 4. Deserialize entries
         │
         ▼
    ┌────────────────┐
    │RecoveryManager │
    └────┬───────────┘
         │
         │ 5. Replay operations
         │
         ▼
    ┌────────────────┐
    │    Storage     │
    │ (ConcurrentMap)│◄───── IN-MEMORY STORAGE
    └────┬───────────┘
         │
         │ 6. Recovery complete
         │
         ▼
    ┌────────────────┐
    │ KeyValueStore  │
    └────┬───────────┘
         │
         │ 7. Ready for operations
         │
         ▼
    ┌──────────┐
    │  Client  │
    └──────────┘
```

---

## WAL File Format

```
┌─────────────────────────────────────────────────────────────┐
│                      wal.log Structure                       │
└─────────────────────────────────────────────────────────────┘

Line 1: [timestamp]|[operation]|[key]|[value]
Line 2: [timestamp]|[operation]|[key]|[value]
Line 3: [timestamp]|[operation]|[key]|[value]
...

Example:
1738419540000|PUT|user:1|John Doe
1738419541000|PUT|user:2|Jane Smith
1738419542000|DELETE|user:1|null
1738419543000|PUT|user:3|Bob Johnson

Format: timestamp|operation|key|value
- timestamp: Unix epoch in milliseconds
- operation: PUT or DELETE
- key: String (no pipes allowed)
- value: String (no pipes allowed) or "null" for DELETE
- Delimiter: | (pipe character)
```

---

## Thread-Safety Model

```
┌─────────────────────────────────────────────────────────────┐
│                   Concurrency Architecture                   │
└─────────────────────────────────────────────────────────────┘

Thread 1                Thread 2                Thread 3
   │                       │                       │
   │ put(k1, v1)          │ get(k2)              │ delete(k3)
   │                       │                       │
   ▼                       ▼                       ▼
┌──────────────────────────────────────────────────────────┐
│              KeyValueStoreImpl                            │
│  ┌────────────────────────────────────────────────┐      │
│  │  synchronized append() to WAL                   │      │
│  │  (Only one thread writes to WAL at a time)     │      │
│  └────────────────────────────────────────────────┘      │
└──────────────────┬───────────────────┬───────────────────┘
                   │                   │
                   ▼                   ▼
         ┌─────────────────┐  ┌──────────────────┐
         │ WriteAheadLog   │  │  InMemoryStorage │
         │                 │  │                  │
         │ synchronized    │  │ ConcurrentHashMap│
         │ append()        │  │ (lock-free reads)│
         └─────────────────┘  └──────────────────┘
                   │                   │
                   │                   │
         Multiple threads can read concurrently
         Only one thread writes to WAL at a time
         ConcurrentHashMap handles storage concurrency


Locking Strategy:
┌─────────────────────────────────────────────────────────────┐
│ Operation  │ WAL Lock      │ Storage Lock                   │
├────────────┼───────────────┼────────────────────────────────┤
│ PUT        │ Synchronized  │ ConcurrentHashMap (lock-free)  │
│ GET        │ None          │ ConcurrentHashMap (lock-free)  │
│ DELETE     │ Synchronized  │ ConcurrentHashMap (lock-free)  │
│ RECOVERY   │ None (startup)│ Single-threaded                │
└─────────────────────────────────────────────────────────────┘
```

---

## SOLID Principles Mapping

```
┌─────────────────────────────────────────────────────────────┐
│         Single Responsibility Principle (SRP)                │
└─────────────────────────────────────────────────────────────┘

KeyValueStoreImpl    ──> Coordinates operations
InMemoryStorage      ──> Manages in-memory data
WriteAheadLogImpl    ──> Handles persistence
RecoveryManagerImpl  ──> Handles recovery logic
LogEntry             ──> Represents log data


┌─────────────────────────────────────────────────────────────┐
│            Open/Closed Principle (OCP)                       │
└─────────────────────────────────────────────────────────────┘

Storage Interface
    │
    ├──> InMemoryStorage (current)
    └──> DiskBasedStorage (future extension)

WriteAheadLog Interface
    │
    ├──> WriteAheadLogImpl (current)
    └──> CompressedWAL (future extension)


┌─────────────────────────────────────────────────────────────┐
│         Liskov Substitution Principle (LSP)                  │
└─────────────────────────────────────────────────────────────┘

Any implementation of Storage can replace InMemoryStorage
Any implementation of WriteAheadLog can replace WriteAheadLogImpl
Contract defined by interface is honored by all implementations


┌─────────────────────────────────────────────────────────────┐
│       Interface Segregation Principle (ISP)                  │
└─────────────────────────────────────────────────────────────┘

Storage           ──> Only storage operations
WriteAheadLog     ──> Only WAL operations
RecoveryManager   ──> Only recovery operations
KeyValueStore     ──> Only KV operations

No fat interfaces - each interface is focused


┌─────────────────────────────────────────────────────────────┐
│        Dependency Inversion Principle (DIP)                  │
└─────────────────────────────────────────────────────────────┘

KeyValueStoreImpl depends on:
    │
    ├──> Storage (interface) not InMemoryStorage (concrete)
    ├──> WriteAheadLog (interface) not WriteAheadLogImpl (concrete)
    └──> RecoveryManager (interface) not RecoveryManagerImpl (concrete)

High-level modules depend on abstractions, not concretions
```

---

## Error Handling Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    Error Scenarios                           │
└─────────────────────────────────────────────────────────────┘

1. WAL Write Failure
   ┌──────────────┐
   │ Client       │
   └──────┬───────┘
          │ put(k, v)
          ▼
   ┌──────────────┐
   │ KVStore      │
   └──────┬───────┘
          │ append()
          ▼
   ┌──────────────┐
   │ WAL          │ ──X──> IOException
   └──────┬───────┘
          │
          │ throw KVDSException
          ▼
   ┌──────────────┐
   │ KVStore      │
   └──────┬───────┘
          │
          │ propagate exception
          ▼
   ┌──────────────┐
   │ Client       │ ──> Operation fails, data not written
   └──────────────┘


2. Corrupted WAL Entry During Recovery
   ┌──────────────┐
   │ Recovery     │
   └──────┬───────┘
          │ readAll()
          ▼
   ┌──────────────┐
   │ WAL          │ ──> Returns entries
   └──────┬───────┘
          │
          │ for each entry
          ▼
   ┌──────────────┐
   │ deserialize()│ ──X──> Parse error
   └──────┬───────┘
          │
          │ Log warning, skip entry
          │ Continue with next entry
          ▼
   ┌──────────────┐
   │ Recovery     │ ──> Partial recovery
   └──────────────┘


3. Key Not Found
   ┌──────────────┐
   │ Client       │
   └──────┬───────┘
          │ get(k)
          ▼
   ┌──────────────┐
   │ Storage      │ ──> Returns null
   └──────┬───────┘
          │
          │ return null (valid response)
          ▼
   ┌──────────────┐
   │ Client       │ ──> Handle null
   └──────────────┘
```

---

## Performance Characteristics

```
┌─────────────────────────────────────────────────────────────┐
│                  Time Complexity                             │
└─────────────────────────────────────────────────────────────┘

Operation     │ Average Case │ Worst Case │ Notes
──────────────┼──────────────┼────────────┼──────────────────
PUT           │ O(1)         │ O(1)       │ + WAL write O(1)
GET           │ O(1)         │ O(1)       │ HashMap lookup
DELETE        │ O(1)         │ O(1)       │ + WAL write O(1)
RECOVERY      │ O(n)         │ O(n)       │ n = # of log entries


┌─────────────────────────────────────────────────────────────┐
│                  Space Complexity                            │
└─────────────────────────────────────────────────────────────┘

Component          │ Space Usage
───────────────────┼────────────────────────────────────────
InMemoryStorage    │ O(n) where n = # of key-value pairs
WAL File           │ O(m) where m = # of operations
Total              │ O(n + m)


┌─────────────────────────────────────────────────────────────┐
│                 Throughput Estimates                         │
└─────────────────────────────────────────────────────────────┘

Operation     │ Estimated Throughput
──────────────┼──────────────────────────────────────
GET           │ 100,000+ ops/sec (in-memory)
PUT           │ 10,000+ ops/sec (limited by disk I/O)
DELETE        │ 10,000+ ops/sec (limited by disk I/O)

Note: Actual throughput depends on hardware and disk speed
```

---

## Deployment View

```
┌─────────────────────────────────────────────────────────────┐
│                    Runtime Environment                       │
└─────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                      JVM (Java 17)                        │
│                                                           │
│  ┌────────────────────────────────────────────────┐      │
│  │         Application Process                     │      │
│  │                                                 │      │
│  │  ┌──────────────────────────────────────┐      │      │
│  │  │  KeyValueStoreImpl Instance          │      │      │
│  │  │                                      │      │      │
│  │  │  Heap Memory:                        │      │      │
│  │  │  - ConcurrentHashMap (data)          │      │      │
│  │  │  - BufferedWriter (WAL)              │      │      │
│  │  └──────────────────────────────────────┘      │      │
│  │                                                 │      │
│  └────────────────────────────────────────────────┘      │
│                                                           │
└───────────────────────┬───────────────────────────────────┘
                        │
                        │ File I/O
                        ▼
┌──────────────────────────────────────────────────────────┐
│                    File System                            │
│                                                           │
│  ./data/wal.log  ◄──── Write-Ahead Log File              │
│                                                           │
└──────────────────────────────────────────────────────────┘


Directory Structure:
KV-DS/
├── data/
│   └── wal.log          ◄── Created at runtime
├── src/
│   └── ...
├── target/
│   └── kv-ds.jar        ◄── Executable JAR
└── pom.xml
```

---

## Testing Strategy Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      Test Pyramid                            │
└─────────────────────────────────────────────────────────────┘

                        ┌─────────┐
                        │  E2E    │  ◄── Full system test
                        │  Tests  │
                        └─────────┘
                      ┌─────────────┐
                      │ Integration │  ◄── Component integration
                      │    Tests    │
                      └─────────────┘
                  ┌───────────────────┐
                  │    Unit Tests     │  ◄── Individual classes
                  │   (80%+ coverage) │
                  └───────────────────┘


Test Coverage:
┌─────────────────────────────────────────────────────────────┐
│ Component              │ Test Type        │ Coverage Goal   │
├────────────────────────┼──────────────────┼─────────────────┤
│ InMemoryStorage        │ Unit             │ 100%            │
│ WriteAheadLogImpl      │ Unit             │ 100%            │
│ LogEntry               │ Unit             │ 100%            │
│ RecoveryManagerImpl    │ Unit             │ 100%            │
│ KeyValueStoreImpl      │ Unit             │ 100%            │
│ WAL + Storage          │ Integration      │ Key scenarios   │
│ Recovery + Storage     │ Integration      │ Key scenarios   │
│ Concurrent Operations  │ Integration      │ Race conditions │
│ Full System            │ E2E              │ Happy path      │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary

This design follows a **layered architecture** with clear separation of concerns:

1. **Core Layer** - Orchestrates operations
2. **Storage Layer** - Manages in-memory data
3. **WAL Layer** - Ensures durability
4. **Recovery Layer** - Restores state on startup

**Key Design Decisions:**
- ✅ Interface-based design for flexibility (SOLID)
- ✅ ConcurrentHashMap for thread-safe storage
- ✅ Synchronized WAL writes for consistency
- ✅ File-based WAL for simplicity
- ✅ Recovery on startup for crash resilience
- ✅ Minimal dependencies for simplicity

**Trade-offs:**
- 📊 Write performance limited by disk I/O (acceptable for MVP)
- 📊 WAL grows indefinitely (no compaction in MVP)
- 📊 Single WAL file (no rotation in MVP)

This design is **simple, testable, and production-ready** for a take-home test! 🚀

---

**Last Updated:** 2026-02-01  
**Version:** 1.0  
**Status:** Ready for Implementation

# KV-DS Implementation Plan

## Project Overview
**Objective:** Build a simple, production-ready Key-Value Data Store with Write-Ahead Logging (WAL) for a take-home test.

**Core Principles:**
- ✅ Keep it Simple, Stupid (KISS)
- ✅ SOLID Principles
- ✅ Incremental Development with Testing
- ✅ In-Memory Storage with WAL for Durability
- ✅ No Extra Features - Focus on Core Functionality

---

## Technology Stack

- **Java:** 17 (LTS)
- **Build Tool:** Maven
- **Testing:** JUnit 5, Mockito
- **Logging:** SLF4J + Logback
- **No Framework:** Pure Java (No Spring Boot to keep it simple)

---

## Core Features (MVP)

### Must-Have:
1. **In-Memory Storage** - ConcurrentHashMap for thread-safety
2. **Basic Operations** - PUT, GET, DELETE
3. **Write-Ahead Log (WAL)** - Durability guarantee
4. **Recovery** - Replay WAL on startup
5. **Thread-Safety** - Concurrent access support
6. **Unit Tests** - For each component

### Explicitly NOT Including:
- ❌ TTL (Time-to-Live)
- ❌ REST API
- ❌ Complex data types
- ❌ Clustering
- ❌ Metrics/Monitoring (beyond basic logging)
- ❌ Snapshots (WAL is sufficient)

---

## SOLID Principles Application

### Single Responsibility Principle (SRP)
- `KeyValueStore` - Manages storage operations only
- `WriteAheadLog` - Handles WAL operations only
- `LogEntry` - Represents a single log entry
- `RecoveryManager` - Handles recovery logic only

### Open/Closed Principle (OCP)
- Use interfaces for extensibility
- `Storage` interface - Can swap implementations
- `Logger` interface - Can change logging strategy

### Liskov Substitution Principle (LSP)
- Any `Storage` implementation should be interchangeable
- Interfaces define contracts that implementations must honor

### Interface Segregation Principle (ISP)
- Small, focused interfaces
- `Storage` - Only storage operations
- `Recoverable` - Only recovery operations

### Dependency Inversion Principle (DIP)
- Depend on abstractions (interfaces), not concrete classes
- `KeyValueStore` depends on `Storage` interface, not `InMemoryStorage`

---

## Project Structure

```
KV-DS/
├── pom.xml
├── README.md
├── Requirement.txt
├── IMPLEMENTATION_PLAN.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── kvds/
    │   │           ├── core/
    │   │           │   ├── KeyValueStore.java          # Main store interface
    │   │           │   └── KeyValueStoreImpl.java      # Implementation
    │   │           ├── storage/
    │   │           │   ├── Storage.java                # Storage interface
    │   │           │   └── InMemoryStorage.java        # ConcurrentHashMap impl
    │   │           ├── wal/
    │   │           │   ├── WriteAheadLog.java          # WAL interface
    │   │           │   ├── WriteAheadLogImpl.java      # WAL implementation
    │   │           │   ├── LogEntry.java               # Log entry model
    │   │           │   └── Operation.java              # Enum: PUT, DELETE
    │   │           ├── recovery/
    │   │           │   ├── RecoveryManager.java        # Recovery interface
    │   │           │   └── RecoveryManagerImpl.java    # Recovery logic
    │   │           └── exception/
    │   │               └── KVDSException.java          # Custom exception
    │   └── resources/
    │       └── logback.xml                             # Logging config
    └── test/
        └── java/
            └── com/
                └── kvds/
                    ├── core/
                    │   └── KeyValueStoreImplTest.java
                    ├── storage/
                    │   └── InMemoryStorageTest.java
                    ├── wal/
                    │   └── WriteAheadLogImplTest.java
                    └── recovery/
                        └── RecoveryManagerImplTest.java
```

---

## Incremental Implementation Steps

### **Phase 1: Project Setup & Basic Storage** (30 mins)
**Goal:** Get the foundation ready

#### Step 1.1: Maven Project Setup
- [ ] Create `pom.xml` with Java 17, JUnit 5, SLF4J, Logback
- [ ] Verify build: `mvn clean compile`

#### Step 1.2: Basic Interfaces
- [ ] Create `Storage` interface (put, get, delete, clear)
- [ ] Create `InMemoryStorage` using `ConcurrentHashMap`
- [ ] Write tests for `InMemoryStorage`
- [ ] **Test:** `mvn test` - All tests pass ✅

---

### **Phase 2: Core KV Store** (30 mins)
**Goal:** Implement main store without persistence

#### Step 2.1: KeyValueStore Interface & Implementation
- [ ] Create `KeyValueStore` interface
- [ ] Create `KeyValueStoreImpl` (delegates to `Storage`)
- [ ] Implement PUT, GET, DELETE operations
- [ ] Write comprehensive unit tests
- [ ] **Test:** `mvn test` - All tests pass ✅

#### Step 2.2: Exception Handling
- [ ] Create `KVDSException` for error handling
- [ ] Add proper exception handling in store operations
- [ ] Test error scenarios
- [ ] **Test:** `mvn test` - All tests pass ✅

---

### **Phase 3: Write-Ahead Log (WAL)** (45 mins)
**Goal:** Add durability through WAL

#### Step 3.1: WAL Data Model
- [ ] Create `Operation` enum (PUT, DELETE)
- [ ] Create `LogEntry` class (operation, key, value, timestamp)
- [ ] Add serialization/deserialization logic
- [ ] Write tests for `LogEntry`
- [ ] **Test:** `mvn test` - All tests pass ✅

#### Step 3.2: WAL Implementation
- [ ] Create `WriteAheadLog` interface (append, readAll, clear)
- [ ] Create `WriteAheadLogImpl` (file-based, append-only)
- [ ] Use `BufferedWriter` for efficient writes
- [ ] Implement fsync for durability
- [ ] Write tests for WAL operations
- [ ] **Test:** `mvn test` - All tests pass ✅

#### Step 3.3: Integrate WAL with KeyValueStore
- [ ] Modify `KeyValueStoreImpl` to log before executing operations
- [ ] Ensure atomicity: log first, then execute
- [ ] Write integration tests
- [ ] **Test:** `mvn test` - All tests pass ✅

---

### **Phase 4: Recovery Mechanism** (30 mins)
**Goal:** Replay WAL on startup

#### Step 4.1: Recovery Manager
- [ ] Create `RecoveryManager` interface
- [ ] Create `RecoveryManagerImpl`
- [ ] Implement WAL replay logic
- [ ] Handle corrupted log entries gracefully
- [ ] Write recovery tests
- [ ] **Test:** `mvn test` - All tests pass ✅

#### Step 4.2: Startup Integration
- [ ] Modify `KeyValueStoreImpl` constructor to trigger recovery
- [ ] Test crash recovery scenarios
- [ ] Verify data integrity after recovery
- [ ] **Test:** `mvn test` - All tests pass ✅

---

### **Phase 5: Thread-Safety & Concurrency** (30 mins)
**Goal:** Ensure safe concurrent access

#### Step 5.1: Concurrency Tests
- [ ] Write multi-threaded test cases
- [ ] Test concurrent PUT operations
- [ ] Test concurrent GET/PUT operations
- [ ] Test concurrent DELETE operations
- [ ] Use `CountDownLatch` and `ExecutorService` for testing
- [ ] **Test:** `mvn test` - All tests pass ✅

#### Step 5.2: Fix Any Race Conditions
- [ ] Identify and fix race conditions (if any)
- [ ] Ensure WAL writes are synchronized
- [ ] **Test:** `mvn test` - All tests pass ✅

---

### **Phase 6: Documentation & Polish** (30 mins)
**Goal:** Make it production-ready

#### Step 6.1: README
- [ ] Write clear setup instructions
- [ ] Add usage examples
- [ ] Document API
- [ ] Include build and test commands

#### Step 6.2: Code Quality
- [ ] Add JavaDoc comments to public APIs
- [ ] Ensure consistent code formatting
- [ ] Remove any dead code
- [ ] Final code review

#### Step 6.3: Final Testing
- [ ] Run full test suite: `mvn clean test`
- [ ] Check test coverage (aim for >80%)
- [ ] Manual integration testing
- [ ] **Test:** All tests pass ✅

---

## Testing Strategy

### Unit Tests (Per Component)
- Test each class in isolation
- Mock dependencies using Mockito
- Cover happy paths and error cases
- Aim for >80% code coverage

### Integration Tests
- Test WAL + Storage integration
- Test Recovery + Storage integration
- Test end-to-end operations

### Concurrency Tests
- Use `ExecutorService` for parallel operations
- Test with 10-100 concurrent threads
- Verify no data corruption

### Test Execution
```bash
# Run all tests
mvn clean test

# Run specific test
mvn test -Dtest=KeyValueStoreImplTest

# Run with coverage
mvn clean test jacoco:report
```

---

## WAL Design Details

### Log Format
```
[TIMESTAMP] [OPERATION] [KEY] [VALUE]
2026-02-01T19:27:00 PUT user:1 {"name":"John"}
2026-02-01T19:27:01 DELETE user:1 null
```

### WAL File Location
- Default: `./data/wal.log`
- Configurable via constructor parameter

### WAL Operations
1. **Append:** Write operation to log file + fsync
2. **Read:** Read all entries for recovery
3. **Clear:** Truncate log (after successful snapshot - not in MVP)

### Recovery Process
1. Read WAL file line by line
2. Parse each log entry
3. Replay operations in order
4. Skip corrupted entries (log warning)
5. Mark recovery complete

---

## Time Estimate

| Phase | Task | Time |
|-------|------|------|
| 1 | Project Setup & Basic Storage | 30 min |
| 2 | Core KV Store | 30 min |
| 3 | Write-Ahead Log | 45 min |
| 4 | Recovery Mechanism | 30 min |
| 5 | Thread-Safety & Concurrency | 30 min |
| 6 | Documentation & Polish | 30 min |
| **Total** | | **~3 hours** |

*Buffer: Add 1-2 hours for debugging and unexpected issues*

---

## Success Criteria

✅ All unit tests pass  
✅ All integration tests pass  
✅ Concurrency tests pass without data corruption  
✅ WAL correctly logs all operations  
✅ Recovery successfully restores state after crash  
✅ Code follows SOLID principles  
✅ Clean, readable code with JavaDoc  
✅ README with clear instructions  
✅ No external frameworks (pure Java)  

---

## Key Design Decisions

### 1. **ConcurrentHashMap for Storage**
- Thread-safe out of the box
- High performance for concurrent reads/writes
- No need for explicit locking

### 2. **File-Based WAL**
- Simple append-only file
- Use `BufferedWriter` for performance
- Call `flush()` + `FileDescriptor.sync()` for durability

### 3. **No Snapshots (MVP)**
- WAL is sufficient for take-home test
- Keeps implementation simple
- Can be added later if needed

### 4. **Minimal Dependencies**
- Only JUnit, Mockito, SLF4J, Logback
- No Spring Boot or heavy frameworks
- Faster build and easier to understand

### 5. **String Keys and Values**
- Simplifies serialization
- Sufficient for demonstration
- Can be generified later

---

## Common Pitfalls to Avoid

❌ **Over-engineering** - Don't add features not in requirements  
❌ **Skipping tests** - Test after each phase  
❌ **Ignoring thread-safety** - Use concurrent data structures  
❌ **Not fsyncing WAL** - Data loss on crash  
❌ **Complex abstractions** - Keep it simple  
❌ **Poor error handling** - Handle corrupted logs gracefully  

---

## Agent Workflow

### For Each Phase:
1. **Implement** - Write minimal code to satisfy requirement
2. **Test** - Write and run tests immediately
3. **Verify** - Ensure `mvn test` passes
4. **Commit** - Mental checkpoint before next phase
5. **Move On** - Don't optimize prematurely

### If Tests Fail:
1. Read error message carefully
2. Fix the issue
3. Re-run tests
4. Don't proceed until green ✅

### If Stuck:
1. Review SOLID principles
2. Check if over-complicating
3. Simplify the approach
4. Focus on making tests pass

---

## Final Deliverables

1. **Source Code** - Clean, well-organized Java code
2. **Tests** - Comprehensive test suite with >80% coverage
3. **README.md** - Setup, usage, and API documentation
4. **pom.xml** - All dependencies and build configuration
5. **Working Application** - Can run and demonstrate all features

---

**Last Updated:** 2026-02-01  
**Estimated Completion Time:** 3-5 hours  
**Complexity:** Medium  
**Focus:** Simplicity, Testing, SOLID Principles, WAL

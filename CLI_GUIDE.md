# KV-DS CLI User Guide

## Overview

KV-DS now includes an **interactive Command-Line Interface (CLI)** that allows you to interact with the key-value store in real-time. The CLI provides a REPL (Read-Eval-Print Loop) with automatic WAL (Write-Ahead Log) and crash recovery.

---

## Quick Start

### Running the CLI

**Windows:**
```bash
kvds.bat
```

**Or manually:**
```bash
# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.2\jbr"

# Compile (if needed)
mvn compile

# Run
java -cp "target/classes" com.kvds.cli.KVDSCli
```

---

## Available Commands

### 1. PUT - Store a Key-Value Pair

**Syntax:**
```
PUT <key> <value>
```

**Examples:**
```
kvds> PUT name Alice
✓ Stored: name = Alice

kvds> PUT age 30
✓ Stored: age = 30

kvds> PUT city New_York
✓ Stored: city = New_York
```

**Notes:**
- Keys and values cannot contain the pipe character `|`
- Values cannot contain newlines
- Spaces in values: use underscores or quotes won't work (limitation)

---

### 2. GET - Retrieve a Value

**Syntax:**
```
GET <key>
```

**Examples:**
```
kvds> GET name
name = Alice

kvds> GET age
age = 30

kvds> GET nonexistent
Key not found: nonexistent
```

---

### 3. DELETE - Remove a Key

**Syntax:**
```
DELETE <key>
```
or
```
DEL <key>
```

**Examples:**
```
kvds> DELETE age
✓ Deleted: age (was: 30)

kvds> DEL city
✓ Deleted: city (was: New_York)

kvds> DELETE nonexistent
✓ Key not found (no-op): nonexistent
```

---

### 4. CLEAR - Remove All Data

**Syntax:**
```
CLEAR
```

**Example:**
```
kvds> CLEAR
✓ All data cleared
```

**Warning:** This removes all keys from the store!

---

### 5. STATUS - Show Store Information

**Syntax:**
```
STATUS
```

**Example:**
```
kvds> STATUS

┌─────────────────────────────────────┐
│         KV-DS Status                │
├─────────────────────────────────────┤
│ WAL Enabled:     YES                │
│ WAL File:        kvds-cli.log       │
│ Storage Type:    In-Memory          │
│ Recovery:        Automatic          │
└─────────────────────────────────────┘
```

---

### 6. HELP - Show Available Commands

**Syntax:**
```
HELP
```
or
```
?
```

**Example:**
```
kvds> HELP
[Displays command reference]
```

---

### 7. EXIT - Quit the CLI

**Syntax:**
```
EXIT
```
or
```
QUIT
```
or
```
Q
```

**Example:**
```
kvds> EXIT

Goodbye! Store closed successfully.
```

---

## Complete Usage Example

Here's a complete session demonstrating all features:

```
╔════════════════════════════════════════════════════════╗
║                                                        ║
║              KV-DS Interactive CLI v1.0                ║
║         Key-Value Data Store with WAL & Recovery       ║
║                                                        ║
╚════════════════════════════════════════════════════════╝

✓ Store initialized with WAL enabled
✓ WAL file: kvds-cli.log
✓ Automatic recovery on startup: ENABLED

kvds> PUT user Alice
✓ Stored: user = Alice

kvds> PUT session abc123
✓ Stored: session = abc123

kvds> PUT login_time 2026-02-01
✓ Stored: login_time = 2026-02-01

kvds> GET user
user = Alice

kvds> GET session
session = abc123

kvds> STATUS

┌─────────────────────────────────────┐
│         KV-DS Status                │
├─────────────────────────────────────┤
│ WAL Enabled:     YES                │
│ WAL File:        kvds-cli.log       │
│ Storage Type:    In-Memory          │
│ Recovery:        Automatic          │
└─────────────────────────────────────┘

kvds> DELETE login_time
✓ Deleted: login_time (was: 2026-02-01)

kvds> GET login_time
Key not found: login_time

kvds> EXIT

Goodbye! Store closed successfully.
```

---

## Crash Recovery Demo

### Step 1: Store Some Data
```
kvds> PUT important_data critical_value
✓ Stored: important_data = critical_value

kvds> PUT user_session xyz789
✓ Stored: user_session = xyz789

kvds> EXIT
Goodbye! Store closed successfully.
```

### Step 2: Restart the CLI
```
kvds.bat
```

### Step 3: Data is Automatically Recovered!
```
✓ Store initialized with WAL enabled
✓ WAL file: kvds-cli.log
✓ Automatic recovery on startup: ENABLED

kvds> GET important_data
important_data = critical_value

kvds> GET user_session
user_session = xyz789
```

**The data persisted across restarts thanks to WAL!** 🎉

---

## Features

### ✅ Automatic WAL (Write-Ahead Log)
- Every PUT and DELETE operation is logged
- Data survives crashes and restarts
- Automatic recovery on startup

### ✅ Interactive REPL
- Type commands and see results immediately
- Command history (use arrow keys)
- Clear, user-friendly output

### ✅ Error Handling
- Invalid commands show helpful error messages
- Validation for keys and values
- Graceful handling of edge cases

### ✅ Simple Commands
- Easy-to-remember command names
- Aliases for common operations (DEL, Q, ?)
- Consistent syntax across all commands

---

## Limitations

1. **Pipe Character:** Keys and values cannot contain `|` (used as WAL delimiter)
2. **Newlines:** Values cannot contain newline characters (line-based WAL format)
3. **Spaces in Values:** Multi-word values need to be entered without spaces or use underscores
4. **In-Memory Only:** Data is stored in memory; WAL is for crash recovery only
5. **Single User:** No concurrent access support in CLI mode

---

## Advanced Usage

### Programmatic Access

You can also use KV-DS as a library in your Java code:

```java
import com.kvds.core.*;
import com.kvds.storage.*;
import com.kvds.wal.*;

// Without WAL
Storage storage = new InMemoryStorage();
KeyValueStore store = new KeyValueStoreImpl(storage);
store.put("key", "value");
String value = store.get("key");
store.close();

// With WAL and Recovery
WriteAheadLog wal = new WriteAheadLogImpl("my-app.log");
Storage storage = new InMemoryStorage();
KeyValueStore store = new KeyValueStoreImpl(storage, wal);
store.put("key", "value");
store.close();
```

---

## Troubleshooting

### Issue: "Command not found"
**Solution:** Make sure you're in the KV-DS directory and `kvds.bat` exists

### Issue: "JAVA_HOME not set"
**Solution:** Edit `kvds.bat` and update the JAVA_HOME path to your Java installation

### Issue: "Class not found"
**Solution:** Run `mvn compile` to compile the project first

### Issue: WAL file locked
**Solution:** Make sure only one instance of the CLI is running at a time

---

## Files Created by CLI

- **kvds-cli.log** - Write-Ahead Log file (stores all operations)
- This file persists data across restarts
- Can be deleted to start fresh (will lose all data)

---

## Comparison: CLI vs Library

| Feature | CLI Mode | Library Mode |
|---------|----------|--------------|
| **Interaction** | Interactive REPL | Programmatic API |
| **Use Case** | Testing, demos, manual operations | Production applications |
| **WAL** | Always enabled | Optional |
| **Recovery** | Automatic | Manual/Automatic |
| **Concurrency** | Single user | Thread-safe (ConcurrentHashMap) |
| **Ease of Use** | Very easy | Requires coding |

---

## Next Steps

1. **Try the CLI:** Run `kvds.bat` and experiment with commands
2. **Test Recovery:** Store data, exit, restart, and verify data persists
3. **Run Tests:** Execute `run-tests.bat` to see all 91 automated tests
4. **Run Demo:** Execute `run-demo.bat` to see comprehensive demos
5. **Read Docs:** Check `README.md` for detailed documentation

---

## Summary

The KV-DS CLI provides:
- ✅ **7 simple commands** (PUT, GET, DELETE, CLEAR, STATUS, HELP, EXIT)
- ✅ **Interactive REPL** for real-time interaction
- ✅ **Automatic WAL** for data durability
- ✅ **Crash recovery** - data persists across restarts
- ✅ **User-friendly** interface with helpful messages
- ✅ **Easy to use** - just run `kvds.bat`

Enjoy using KV-DS! 🚀

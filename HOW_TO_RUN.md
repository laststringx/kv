# 🚀 KV-DS - How to Run and Test

## Quick Start - Run the CLI NOW!

### Step 1: Open Command Prompt
Press `Win + R`, type `cmd`, press Enter

### Step 2: Navigate to Project
```bash
cd c:\Users\pratt\source\repos\KV-DS
```

### Step 3: Run the CLI
```bash
kvds.bat
```

You should see:
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

kvds>
```

---

## 20 Ready-to-Use Test Commands

### Just Copy and Paste These Commands One by One:

#### Test 1: Basic Operations
```
PUT username JohnDoe
GET username
```
**Expected:** `✓ Stored: username = JohnDoe` then `username = JohnDoe`

---

#### Test 2: Store Email
```
PUT email john@example.com
GET email
```
**Expected:** `email = john@example.com`

---

#### Test 3: Store Multiple Values
```
PUT age 25
PUT city NewYork
PUT country USA
```
**Expected:** Three success messages

---

#### Test 4: Retrieve Multiple Values
```
GET age
GET city
GET country
```
**Expected:** `25`, `NewYork`, `USA`

---

#### Test 5: Update a Value
```
PUT status pending
GET status
PUT status completed
GET status
```
**Expected:** First shows `pending`, then `completed`

---

#### Test 6: Delete Operation
```
PUT temp_data temporary
GET temp_data
DELETE temp_data
GET temp_data
```
**Expected:** First shows `temporary`, then `Key not found`

---

#### Test 7: Non-Existent Key
```
GET nonexistent_key
```
**Expected:** `Key not found: nonexistent_key`

---

#### Test 8: Store Numbers
```
PUT count 100
PUT price 99.99
PUT year 2026
```
**Expected:** All stored successfully

---

#### Test 9: Retrieve Numbers
```
GET count
GET price
GET year
```
**Expected:** `100`, `99.99`, `2026`

---

#### Test 10: Underscores in Values
```
PUT full_name John_Doe_Smith
GET full_name
```
**Expected:** `John_Doe_Smith`

---

#### Test 11: Dashes in Values
```
PUT phone-number 555-1234
PUT date 2026-02-01
GET phone-number
```
**Expected:** `555-1234`

---

#### Test 12: Session Data
```
PUT session_id abc123xyz
PUT user_id 12345
PUT login_time 23:30:00
GET session_id
```
**Expected:** `abc123xyz`

---

#### Test 13: Multiple Deletes
```
PUT key1 value1
PUT key2 value2
PUT key3 value3
DELETE key1
DELETE key2
GET key3
```
**Expected:** `key3 = value3` (others deleted)

---

#### Test 14: PUT-DELETE-PUT Sequence
```
PUT test_key first_value
DELETE test_key
PUT test_key second_value
GET test_key
```
**Expected:** `second_value`

---

#### Test 15: Configuration Data
```
PUT db_host localhost
PUT db_port 5432
PUT db_name myapp
GET db_host
```
**Expected:** `localhost`

---

#### Test 16: Check Status
```
STATUS
```
**Expected:** Shows WAL enabled, file name, etc.

---

#### Test 17: Special Characters
```
PUT symbol_test value@#$%
GET symbol_test
```
**Expected:** `value@#$%`

---

#### Test 18: Overwrite Multiple Times
```
PUT counter 1
PUT counter 2
PUT counter 3
GET counter
```
**Expected:** `3`

---

#### Test 19: Long Values
```
PUT description ThisIsAVeryLongValueWithoutSpaces123456789
GET description
```
**Expected:** Full value returned

---

#### Test 20: Clear All Data
```
PUT before_clear value1
PUT another_key value2
CLEAR
GET before_clear
```
**Expected:** `Key not found` after CLEAR

---

## Crash Recovery Test (Most Important!)

### Session 1:
```
PUT important_data critical_value
PUT user_session xyz789
PUT timestamp 2026-02-01
GET important_data
EXIT
```

### Session 2 (Restart CLI):
```
kvds.bat
GET important_data
GET user_session
GET timestamp
```

**Expected:** All three values are recovered! 🎉

---

## All Commands in One Block (Copy-Paste Friendly)

```
PUT username JohnDoe
GET username
PUT email john@example.com
GET email
PUT age 25
PUT city NewYork
GET age
GET city
PUT status pending
GET status
PUT status completed
GET status
PUT temp_data temporary
DELETE temp_data
GET temp_data
GET nonexistent_key
PUT count 100
PUT price 99.99
GET count
GET price
PUT full_name John_Doe_Smith
GET full_name
PUT phone-number 555-1234
GET phone-number
PUT session_id abc123xyz
GET session_id
PUT key1 value1
PUT key2 value2
DELETE key1
GET key1
GET key2
PUT test_key first_value
DELETE test_key
PUT test_key second_value
GET test_key
PUT db_host localhost
GET db_host
STATUS
PUT symbol_test value@#$%
GET symbol_test
PUT counter 1
PUT counter 2
PUT counter 3
GET counter
PUT description ThisIsAVeryLongValue
GET description
PUT before_clear value1
CLEAR
GET before_clear
HELP
EXIT
```

---

## Alternative: Run Tests Automatically

If you want to see automated tests instead:

```bash
run-tests.bat
```

This runs all 91 automated tests and shows results.

---

## Alternative: Run Demo

To see a visual demonstration:

```bash
run-demo.bat
```

This runs 5 comprehensive test suites automatically.

---

## Summary

**To interact with KV-DS:**

1. **Interactive CLI** (Manual testing):
   ```bash
   kvds.bat
   ```
   Then paste commands from above

2. **Automated Tests** (91 tests):
   ```bash
   run-tests.bat
   ```

3. **Visual Demo** (5 test suites):
   ```bash
   run-demo.bat
   ```

**All three methods work!** Choose based on your preference:
- CLI = Interactive, manual control
- Tests = Automated, comprehensive
- Demo = Visual, quick overview

---

## Troubleshooting

**If `kvds.bat` doesn't work:**

Try running manually:
```bash
cd c:\Users\pratt\source\repos\KV-DS
mvn compile
java -cp "target/classes" com.kvds.cli.KVDSCli
```

**If you see "Class not found":**
```bash
mvn clean compile
```

Then try again.

---

## What You'll See

When you run `kvds.bat`, you'll see a beautiful interface like this:

```
╔════════════════════════════════════════════════════════╗
║              KV-DS Interactive CLI v1.0                ║
╚════════════════════════════════════════════════════════╝

✓ Store initialized with WAL enabled

kvds> PUT name Alice
✓ Stored: name = Alice

kvds> GET name
name = Alice

kvds> STATUS
┌─────────────────────────────────────┐
│         KV-DS Status                │
├─────────────────────────────────────┤
│ WAL Enabled:     YES                │
│ WAL File:        kvds-cli.log       │
└─────────────────────────────────────┘

kvds> EXIT
Goodbye! Store closed successfully.
```

**Ready to test? Run `kvds.bat` now!** 🚀

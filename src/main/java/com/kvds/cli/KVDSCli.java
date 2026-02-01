package com.kvds.cli;

import com.kvds.core.KeyValueStore;
import com.kvds.core.KeyValueStoreImpl;
import com.kvds.storage.InMemoryStorage;
import com.kvds.storage.Storage;
import com.kvds.wal.WriteAheadLog;
import com.kvds.wal.WriteAheadLogImpl;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Interactive Command-Line Interface for KV-DS.
 * Provides a REPL (Read-Eval-Print Loop) for interacting with the key-value store.
 */
public class KVDSCli {
    
    private static final String WAL_FILE = "kvds-cli.log";
    private static KeyValueStore store;
    private static boolean walEnabled = true;
    
    public static void main(String[] args) throws IOException {
        printBanner();
        
        // Initialize store with WAL
        initializeStore();
        
        // Start REPL
        runREPL();
    }
    
    private static void printBanner() {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║                                                        ║");
        System.out.println("║              KV-DS Interactive CLI v1.0                ║");
        System.out.println("║         Key-Value Data Store with WAL & Recovery       ║");
        System.out.println("║                                                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static void initializeStore() throws IOException {
        Storage storage = new InMemoryStorage();
        
        if (walEnabled) {
            WriteAheadLog wal = new WriteAheadLogImpl(WAL_FILE);
            store = new KeyValueStoreImpl(storage, wal);
            System.out.println("✓ Store initialized with WAL enabled");
            System.out.println("✓ WAL file: " + WAL_FILE);
            System.out.println("✓ Automatic recovery on startup: ENABLED");
        } else {
            store = new KeyValueStoreImpl(storage);
            System.out.println("✓ Store initialized (WAL disabled)");
        }
        
        System.out.println();
    }
    
    private static void runREPL() {
        Scanner scanner = new Scanner(System.in);
        
        printHelp();
        
        while (true) {
            System.out.print("\nkvds> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            try {
                if (!processCommand(input)) {
                    break; // Exit command
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
        
        // Cleanup
        store.close();
        scanner.close();
        System.out.println("\nGoodbye! Store closed successfully.");
    }
    
    private static boolean processCommand(String input) {
        String[] parts = input.split("\\s+", 3);
        String command = parts[0].toUpperCase();
        
        switch (command) {
            case "PUT":
                handlePut(parts);
                break;
                
            case "GET":
                handleGet(parts);
                break;
                
            case "DELETE":
            case "DEL":
                handleDelete(parts);
                break;
                
            case "CLEAR":
                handleClear();
                break;
                
            case "HELP":
            case "?":
                printHelp();
                break;
                
            case "STATUS":
                printStatus();
                break;
                
            case "EXIT":
            case "QUIT":
            case "Q":
                return false;
                
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands");
        }
        
        return true;
    }
    
    private static void handlePut(String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: PUT <key> <value>");
            return;
        }
        
        String key = parts[1];
        String value = parts[2];
        
        store.put(key, value);
        System.out.println("✓ Stored: " + key + " = " + value);
    }
    
    private static void handleGet(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: GET <key>");
            return;
        }
        
        String key = parts[1];
        String value = store.get(key);
        
        if (value != null) {
            System.out.println(key + " = " + value);
        } else {
            System.out.println("Key not found: " + key);
        }
    }
    
    private static void handleDelete(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: DELETE <key>");
            return;
        }
        
        String key = parts[1];
        String oldValue = store.get(key);
        
        store.delete(key);
        
        if (oldValue != null) {
            System.out.println("✓ Deleted: " + key + " (was: " + oldValue + ")");
        } else {
            System.out.println("✓ Key not found (no-op): " + key);
        }
    }
    
    private static void handleClear() {
        store.clear();
        System.out.println("✓ All data cleared");
    }
    
    private static void printStatus() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│         KV-DS Status                │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ WAL Enabled:     " + (walEnabled ? "YES" : "NO") + "              │");
        System.out.println("│ WAL File:        " + WAL_FILE + "   │");
        System.out.println("│ Storage Type:    In-Memory          │");
        System.out.println("│ Recovery:        Automatic          │");
        System.out.println("└─────────────────────────────────────┘");
    }
    
    private static void printHelp() {
        System.out.println("\n┌────────────────────────────────────────────────────────┐");
        System.out.println("│                  Available Commands                    │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│                                                        │");
        System.out.println("│  PUT <key> <value>    Store a key-value pair          │");
        System.out.println("│  GET <key>            Retrieve value for a key        │");
        System.out.println("│  DELETE <key>         Delete a key (alias: DEL)       │");
        System.out.println("│  CLEAR                Clear all data                  │");
        System.out.println("│                                                        │");
        System.out.println("│  STATUS               Show store status               │");
        System.out.println("│  HELP                 Show this help (alias: ?)       │");
        System.out.println("│  EXIT                 Exit the CLI (alias: QUIT, Q)   │");
        System.out.println("│                                                        │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│                      Examples                          │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│                                                        │");
        System.out.println("│  PUT name Alice                                        │");
        System.out.println("│  PUT age 30                                            │");
        System.out.println("│  GET name                                              │");
        System.out.println("│  DELETE age                                            │");
        System.out.println("│  CLEAR                                                 │");
        System.out.println("│                                                        │");
        System.out.println("└────────────────────────────────────────────────────────┘");
    }
}

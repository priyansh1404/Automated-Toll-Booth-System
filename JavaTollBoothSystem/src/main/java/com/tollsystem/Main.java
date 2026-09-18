package com.tollsystem;

import com.tollsystem.entity.VehiclePassEntity;
import com.tollsystem.model.*;
import com.tollsystem.repository.DatabaseManager;
import com.tollsystem.service.AuditLoggerService;
import com.tollsystem.service.ConcurrentLaneService;
import com.tollsystem.service.TollManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static TollManager tollManager = TollManager.getInstance();
    private static AuditLoggerService auditLogger = new AuditLoggerService();
    private static ConcurrentLaneService laneService = new ConcurrentLaneService(3, auditLogger);
    private static DatabaseManager dbManager;

    public static void main(String[] args) {
        System.out.println("Automated Toll Booth & Vehicle Management System");
        System.out.println("Initializing database...");
        dbManager = new DatabaseManager();

        // Seed some sample vehicles so lanes aren't empty on startup
        seedInitialVehicles();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readInt(scanner, "Enter choice: ");

            switch (choice) {
                case 1:
                    registerVehicle(scanner);
                    break;
                case 2:
                    processLanes(scanner);
                    break;
                case 3:
                    viewQueuesAndHistory();
                    break;
                case 4:
                    viewAuditLog(scanner);
                    break;
                case 5:
                    databaseMenu(scanner);
                    break;
                case 6:
                    viewStats();
                    break;
                case 7:
                    runAutomatedDemo();
                    break;
                case 8:
                    resetSystem(scanner);
                    break;
                case 0:
                    running = false;
                    cleanup();
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 0 and 8.");
            }

            if (running) {
                System.out.print("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }

        scanner.close();
        System.out.println("System exited.");
    }

    private static void printMenu() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("                  MAIN MENU                       ");
        System.out.println("--------------------------------------------------");
        System.out.println("  1. Register a new vehicle                        ");
        System.out.println("  2. Process concurrent toll lanes                 ");
        System.out.println("  3. View lane queues & transaction history         ");
        System.out.println("  4. Export & read file audit log                  ");
        System.out.println("  5. Database operations (JDBC / JPA)              ");
        System.out.println("  6. View system statistics report                 ");
        System.out.println("  7. Run end-to-end automated demo                 ");
        System.out.println("  8. Reset system (stats & queues)                 ");
        System.out.println("  0. Exit                                           ");
        System.out.println("--------------------------------------------------");
    }

    private static void registerVehicle(Scanner scanner) {
        System.out.println("\nRegister New Vehicle");
        System.out.println("1. Car | 2. Truck | 3. Bus");
        int typeChoice = readInt(scanner, "Select type (1-3): ");

        System.out.print("Registration number: ");
        String regNum = scanner.nextLine().trim().toUpperCase();
        if (regNum.isEmpty()) {
            System.out.println("Registration number cannot be empty.");
            return;
        }

        double balance = readDouble(scanner, "Wallet balance (INR): ");

        System.out.print("FASTag enabled? (y/n): ");
        boolean fastag = scanner.nextLine().trim().equalsIgnoreCase("y");

        Vehicle vehicle = null;
        if (typeChoice == 1) {
            System.out.print("Is commercial / taxi? (y/n): ");
            boolean commercial = scanner.nextLine().trim().equalsIgnoreCase("y");
            vehicle = new Car(regNum, balance, fastag, commercial);
        } else if (typeChoice == 2) {
            int axles = readInt(scanner, "Number of axles: ");
            double weight = readDouble(scanner, "Weight in tonnes: ");
            try {
                vehicle = new Truck(regNum, balance, fastag, weight, axles);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
        } else if (typeChoice == 3) {
            int seats = readInt(scanner, "Seating capacity: ");
            System.out.print("Government / state transport bus? (y/n): ");
            boolean govt = scanner.nextLine().trim().equalsIgnoreCase("y");
            try {
                vehicle = new Bus(regNum, balance, fastag, seats, govt);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("Unknown vehicle type.");
            return;
        }

        if (tollManager.registerVehicle(vehicle)) {
            System.out.printf("Registered: %s (Toll fee: INR %.2f)%n", vehicle.getRegistrationNum(), vehicle.calculateToll());
            System.out.print("Queue this vehicle into a lane now? (y/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                laneService.addVehicleToShortestLane(vehicle);
            }
        } else {
            System.out.println("Vehicle with registration " + regNum + " is already registered.");
        }
    }

    private static void processLanes(Scanner scanner) {
        int totalQueued = laneService.getTotalQueuedVehicles();
        if (totalQueued == 0) {
            System.out.println("No vehicles are currently waiting in any lane.");
            return;
        }

        System.out.printf("Ready to process %d vehicle(s) across %d lanes.%n",
                totalQueued, laneService.getActiveLaneCount());
        System.out.print("Start processing? (y/n): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Operation cancelled.");
            return;
        }

        laneService.processAllLanesConcurrently();

        System.out.print("\nSave processed transactions to database? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            List<Transaction> history = new ArrayList<>(laneService.getTransactionHistory());
            int saved = dbManager.bulkInsertTransactions(history);
            System.out.printf("Saved %d records to database.%n", saved);
        }

        System.out.print("Clear lane queues for next batch? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            laneService.clearQueues();
            System.out.println("Lane queues cleared.");
        }
    }

    private static void viewQueuesAndHistory() {
        System.out.println("\nRegistered Vehicles (" + tollManager.getVehicleRegistry().size() + "):");
        if (tollManager.getVehicleRegistry().isEmpty()) {
            System.out.println("  (None)");
        } else {
            for (Vehicle v : tollManager.getVehicleRegistry().values()) {
                System.out.println("  " + v);
            }
        }

        laneService.displayAllLaneQueues();
        laneService.displayTransactionHistory();
    }

    private static void viewAuditLog(Scanner scanner) {
        System.out.println("\nAudit Log Options:");
        System.out.println("1. Display complete log file");
        System.out.println("2. Display transaction entries only");
        System.out.println("3. Append custom administrative event");
        System.out.println("4. Clear audit log file");
        int option = readInt(scanner, "Choose option (1-4): ");

        if (option == 1) {
            auditLogger.exportAndDisplayLog();
        } else if (option == 2) {
            List<String> txns = auditLogger.readTransactionLines();
            System.out.println("\nTransactions found in log (" + txns.size() + "):");
            if (txns.isEmpty()) {
                System.out.println("  (No transactions logged yet)");
            } else {
                for (String line : txns) {
                    System.out.println("  " + line);
                }
            }
        } else if (option == 3) {
            System.out.print("Enter event description: ");
            String msg = scanner.nextLine().trim();
            auditLogger.logSystemEvent(msg);
            System.out.println("Event written to log.");
        } else if (option == 4) {
            System.out.print("Are you sure you want to clear the audit log? (yes/no): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("yes")) {
                auditLogger.clearLog();
            } else {
                System.out.println("Log clearing cancelled.");
            }
        } else {
            System.out.println("Invalid option.");
        }
    }

    private static void databaseMenu(Scanner scanner) {
        System.out.println("\nDatabase Operations:");
        System.out.println("1. Test JDBC connection");
        System.out.println("2. Run JDBC revenue summary query");
        System.out.println("3. Show all JPA pass records");
        System.out.println("4. Find passes by vehicle registration (JPQL)");
        int choice = readInt(scanner, "Select option (1-4): ");

        switch (choice) {
            case 1:
                boolean connected = dbManager.testJdbcConnection();
                System.out.println(connected ? "JDBC connection is working." : "JDBC connection failed.");
                break;
            case 2:
                dbManager.printRevenueSummaryViaJdbc();
                break;
            case 3:
                dbManager.displayAllPassRecords();
                break;
            case 4:
                System.out.print("Enter registration number: ");
                String reg = scanner.nextLine().trim().toUpperCase();
                List<VehiclePassEntity> passes = dbManager.fetchPassesByVehicle(reg);
                System.out.printf("Pass records for %s (%d found):%n", reg, passes.size());
                for (VehiclePassEntity p : passes) {
                    System.out.println("  " + p);
                }
                break;
            default:
                System.out.println("Invalid selection.");
        }
    }

    private static void viewStats() {
        System.out.println(tollManager.generateSummaryReport());
        System.out.println("Current Session Ledger: " + laneService.getRevenueLedger());
    }

    private static void runAutomatedDemo() {
        System.out.println("\n========================================================");
        System.out.println("     RUNNING END-TO-END AUTOMATED SYSTEM DEMO (OPTION 7) ");
        System.out.println("========================================================");

        // Step 1: Clean reset for a predictable demo state
        tollManager.resetAll();
        laneService.resetAll();
        auditLogger.logSystemEvent("Automated end-to-end demo initiated.");

        // Step 2: Register 8 test vehicles across all categories and edge cases
        System.out.println("\n--- 1. Registering 8 Test Vehicles (All Categories & Edge Cases) ---");
        Vehicle[] testVehicles = {
            new Car("MH12AA1001", 500.0, true, false),            // Standard Private Car (INR 80.00)
            new Car("DL01TA2002", 300.0, true, true),             // Commercial / Taxi Car (INR 120.00)
            new Car("MH14BB3003", 30.0, true, false),             // Low balance -> InsufficientBalanceException
            new Car("HR26CC4004", 500.0, false, false),           // Inactive FASTag -> UnauthorizedVehicleException
            new Truck("GJ01ZZ5005", 1500.0, true, 8.0, 3),        // Standard Truck: 3 axles, 8T (INR 450.00)
            new Truck("RJ14TR6006", 2500.0, true, 16.0, 5),       // Overloaded Truck: 5 axles, 16T (INR 1050.00)
            new Bus("UP32BS7007", 800.0, true, 40, false),        // Standard Bus: 40 seats, private (INR 200.00)
            new Bus("KA04GT8008", 600.0, true, 50, true)          // Large Govt Bus: 50 seats (INR 300 * 0.75 = INR 225.00)
        };

        for (Vehicle v : testVehicles) {
            tollManager.registerVehicle(v);
            laneService.addVehicleToShortestLane(v);
        }

        // Step 3: Display lane queues before processing
        System.out.println("\n--- 2. Lane Queues Before Concurrent Processing ---");
        laneService.displayAllLaneQueues();

        // Step 4: Run concurrent multi-lane processing
        System.out.println("\n--- 3. Launching Concurrent Lane Workers (Threads) ---");
        laneService.processAllLanesConcurrently();

        // Step 5: Display LIFO transaction history from Stack
        System.out.println("\n--- 4. LIFO Transaction History Stack ---");
        laneService.displayTransactionHistory();

        // Step 6: JPA bulk persistence
        System.out.println("\n--- 5. JPA Bulk Persistence ---");
        List<Transaction> history = new ArrayList<>(laneService.getTransactionHistory());
        int saved = dbManager.bulkInsertTransactions(history);
        System.out.printf("[JPA] Bulk persisted %d transaction record(s) into H2 database.%n", saved);

        // Step 7: Native JDBC aggregation query
        System.out.println("\n--- 6. Direct JDBC Aggregation Summary Report ---");
        dbManager.printRevenueSummaryViaJdbc();

        // Step 8: File audit log dump
        System.out.println("\n--- 7. File Audit Trail Verification (toll_audit_log.txt) ---");
        auditLogger.exportAndDisplayLog();

        // Step 9: Global statistics report
        System.out.println("\n--- 8. Global Plaza Statistics Report ---");
        viewStats();

        auditLogger.logSystemEvent("Automated end-to-end demo completed successfully.");
        System.out.println("\n========================================================");
        System.out.println("     AUTOMATED DEMO COMPLETED SUCCESSFULLY!            ");
        System.out.println("========================================================");
    }

    private static void resetSystem(Scanner scanner) {
        System.out.print("Reset active lane queues and session stats? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("Reset cancelled.");
            return;
        }

        tollManager.resetAll();
        laneService.resetAll();
        System.out.println("Session queues and stats have been reset.");

        System.out.print("Re-seed sample vehicles into lanes? (yes/no): ");
        String reseed = scanner.nextLine().trim();
        if (reseed.equalsIgnoreCase("yes")) {
            seedInitialVehicles();
            System.out.println("Sample vehicles re-seeded.");
        }
    }

    private static void seedInitialVehicles() {
        Vehicle[] sample = {
            new Car("MH12AA1001", 500.0, true, false),
            new Car("MH12AA1002", 40.0, true, false),   // will fail: insufficient balance
            new Truck("GJ01ZZ9001", 1400.0, true, 14.0, 4),
            new Bus("DL03BU3001", 750.0, true, 50, false),
            new Bus("KA04GT4001", 300.0, true, 32, true)
        };

        for (Vehicle v : sample) {
            if (tollManager.registerVehicle(v)) {
                laneService.addVehicleToShortestLane(v);
            }
        }
    }

    private static void cleanup() {
        auditLogger.logSystemEvent("System shutting down.");
        if (dbManager != null) {
            dbManager.close();
        }
    }

    // Helper methods for clean and repetitive input parsing
    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid decimal number.");
            }
        }
    }
}
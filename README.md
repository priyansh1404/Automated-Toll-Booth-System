# Automated Toll Booth & Vehicle Management System

A multi-threaded Java simulation of an automated highway toll plaza (inspired by electronic toll collection systems like FASTag). The project models vehicles arriving at multiple toll gates simultaneously, computes tariffs dynamically using vehicle polymorphism, handles wallet balance and tag authorization checks, logs transactions safely across concurrent threads, writes audit trails to disk, and persists records using both JPA/Hibernate and JDBC.

---

## Project Overview

This project was developed to demonstrate practical, real-world application of core and advanced Java concepts. The entire codebase is organized into clear packages under `com.tollsystem` and spans five key areas of the Java curriculum:

1. **Unit 1 & 2 — OOP Design & Domain Models (`com.tollsystem.model`)**
   - Abstract `Vehicle` base class utilizing the Template Method pattern: `processPayment()` standardizes the deduction steps while subclasses provide concrete implementations of `calculateToll()`.
   - Concrete implementations for `Car`, `Truck`, and `Bus` override toll calculation based on vehicle attributes (flat rate, commercial multiplier, axle count, weight thresholds, seating capacity, and government discounts).
   - Strongly typed categories via the `VehicleType` enum (`CAR`, `TRUCK`, `BUS`).
   - A thread-safe `TollManager` singleton implemented with double-checked locking (`volatile`) to track system-wide statistics, manage vehicle registrations, and generate unique transaction IDs using `AtomicLong`.

2. **Unit 3 — Concurrency & Custom Exceptions (`com.tollsystem.service`, `com.tollsystem.exception`)**
   - `LaneWorker` implements `Runnable`, allowing separate toll lanes to process queues in parallel on independent threads.
   - Shared revenue state in `TollRevenueLedger` is protected by `synchronized` methods to prevent race conditions during concurrent transactions.
   - Custom checked exceptions (`InsufficientBalanceException` and `UnauthorizedVehicleException`) trap runtime payment or tag authorization failures gracefully without crashing worker threads.

3. **Unit 4 — Data Structures & File I/O (`com.tollsystem.service`)**
   - Live queues at each lane are managed using dynamic `ArrayList<Vehicle>` structures.
   - Completed transactions are pushed onto a LIFO `Stack<Transaction>` to keep a running history of recent passes.
   - `AuditLoggerService` streams transaction records to a local flat file (`toll_audit_log.txt`) using `BufferedWriter` in append mode and reads them back via `BufferedReader`, with support for viewing, filtering, administrative event logging, and clearing.

4. **Unit 5 — Persistence Layer with JPA & JDBC (`com.tollsystem.entity`, `com.tollsystem.repository`)**
   - `VehiclePassEntity` maps transaction data to the relational `VEHICLE_PASS` table using standard JPA annotations (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`), accurately recording toll amounts, balances, status, and whether FASTag was active.
   - `DatabaseManager` integrates JPA/Hibernate (`EntityManagerFactory`, JPQL queries, transactions) for entity CRUD operations alongside native JDBC (`Connection`, `Statement`, `ResultSet`) for aggregate summary queries (`GROUP BY`, `SUM`, `AVG`).
   - Operates with an embedded in-memory H2 database (`jdbc:h2:mem:tolldb`), running with zero external database configuration required.

---

## Key Features

- **Multi-Lane Concurrency:** Multiple toll lanes process vehicles in parallel threads while distributing new traffic to the shortest queue.
- **Thread-Safe Revenue Accounting:** The shared financial ledger uses synchronized operations to prevent lost updates when lanes finish transactions at the same instant.
- **Polymorphic Toll Calculations:**
  - **Car:** Flat rate of ₹80.00 (₹120.00 for commercial/taxi vehicles).
  - **Truck:** ₹150.00 per axle, plus ₹50.00 per tonne for weight exceeding 10 tonnes.
  - **Bus:** Tiered by seating capacity (≤20 seats: ₹100; 21–45 seats: ₹200; >45 seats: ₹300), with a 25% discount applied for state-run buses.
- **Custom Checked Exceptions:**
  - `InsufficientBalanceException`: Identifies wallet shortfalls and calculates the deficit amount without terminating the lane loop.
  - `UnauthorizedVehicleException`: Flags missing or inactive FASTags and routes vehicles to manual booths.
- **Audit Logging via File I/O:** Every transaction is written to `toll_audit_log.txt` with pipe-separated fields (ID, registration, vehicle type, toll amount, balance, lane, timestamp, status, and message).
- **Dual-Layer Database Operations:**
  - JPA / Hibernate for managing entity records and executing JPQL queries.
  - JDBC queries for generating quick plaza revenue summaries.
- **Interactive CLI & Automated Demo:** An intuitive console menu for manual operations, along with a full automated test run (Option 7) that executes the entire pipeline end-to-end.

---

## Tech Stack & Tools

- **Language:** Java 17+ (compatible with Java 11 up to Java 26)
- **Concurrency:** Java Threads, `Runnable`, `synchronized` locks, `AtomicLong`
- **Collections:** `ArrayList`, `Stack`, `Map`, `Collections.synchronizedMap`
- **I/O Streams:** `BufferedWriter`, `BufferedReader`, `FileWriter`, `FileReader`
- **Persistence (JPA):** JPA 2.2 (`javax.persistence`) with Hibernate ORM 5.6.15.Final
- **Database:** Embedded H2 Database (2.2.x in-memory mode)
- **Direct SQL:** JDBC (`java.sql.*`)
- **Build Tool:** Apache Maven 3.8+ (with bundled libraries in `lib/` for standalone compilation)

---

## Project Directory Structure

```
JavaTollBoothSystem/
├── pom.xml                                    # Maven dependencies & build configuration
├── README.md                                  # Project overview and run guide
├── statement.md                               # Problem statement & scope specification
├── toll_audit_log.txt                         # Generated runtime audit log file
├── lib/                                       # Bundled dependencies for offline compilation
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── tollsystem/
        │           ├── Main.java              # Console UI & application entry point
        │           ├── model/
        │           │   ├── Vehicle.java       # Abstract base class
        │           │   ├── Car.java           # Car toll implementation
        │           │   ├── Truck.java         # Truck axle/weight toll implementation
        │           │   ├── Bus.java           # Bus capacity-based toll implementation
        │           │   ├── Transaction.java   # Transaction value object
        │           │   └── VehicleType.java   # CAR, TRUCK, BUS enum
        │           ├── exception/
        │           │   ├── InsufficientBalanceException.java
        │           │   └── UnauthorizedVehicleException.java
        │           ├── service/
        │           │   ├── TollManager.java           # Singleton stats & vehicle registry
        │           │   ├── LaneWorker.java            # Runnable lane worker
        │           │   ├── TollRevenueLedger.java     # Synchronized revenue ledger
        │           │   ├── ConcurrentLaneService.java # Thread launcher & queue manager
        │           │   └── AuditLoggerService.java    # File read/write operations
        │           ├── entity/
        │           │   └── VehiclePassEntity.java     # JPA entity mapping
        │           └── repository/
        │               └── DatabaseManager.java       # JDBC & JPA operations
        └── resources/
            └── META-INF/
                └── persistence.xml            # Hibernate & H2 configuration
```

---

## How to Set Up and Run

### Prerequisites
- JDK 11 or higher installed (`java -version`)
- Apache Maven 3.8+ installed (optional if using standalone compilation)

---

### Option 1: Build and Run with Maven (Recommended)

1. Open your terminal and navigate to the project root directory:
   ```bash
   cd JavaTollBoothSystem
   ```

2. Compile and package the executable fat JAR:
   ```bash
   mvn clean package
   ```
   *(This downloads dependencies, compiles classes, and packages a self-contained JAR inside `target/`.)*

3. Run the application:
   ```bash
   java -jar target/JavaTollBoothSystem-1.0.0.jar
   ```

---

### Option 2: Compile and Run Manually (Command Line)

If you prefer compiling directly using `javac` without Maven, you can compile using the pre-bundled libraries in `lib/`:

1. Compile source files into a `bin` directory:
   ```powershell
   # Create output folder
   New-Item -ItemType Directory -Force -Path "bin"

   # Compile source code with bundled libraries
   javac -cp "lib/*" -d bin (Get-ChildItem -Recurse -Include *.java src/main/java).FullName

   # Copy persistence.xml into bin
   Copy-Item -Recurse -Force src/main/resources/* bin/
   ```

2. Run the compiled classes:
   ```powershell
   java -cp "bin;lib/*" com.tollsystem.Main
   ```

*(Alternatively, if running the pre-built fat JAR directly:)*
```powershell
java -jar target/JavaTollBoothSystem-1.0.0.jar
```

---

## Testing Guide

### 1. Manual Testing via Interactive Menu
When launched, the system automatically initializes the H2 database, creates the required schema, queues 5 sample demo vehicles, and presents the main menu:

```
--------------------------------------------------
                  MAIN MENU                       
--------------------------------------------------
  1. Register a new vehicle                        
  2. Process concurrent toll lanes                 
  3. View lane queues & transaction history         
  4. Export & read file audit log                  
  5. Database operations (JDBC / JPA)              
  6. View system statistics report                 
  7. Run end-to-end automated demo                 
  8. Reset system (stats & queues)                 
  0. Exit                                           
--------------------------------------------------
```

You can test each module through the menu:
- **Option 1:** Add a new Car, Truck, or Bus with custom attributes (registration, balance, axles, weight, seats) and enqueue it into a lane.
- **Option 2:** Trigger multi-threaded processing. Watch each lane worker deduct tolls on separate threads, handle balance issues, update the synchronized ledger, and prompt to save transactions to the database.
- **Option 3:** View live vehicle queues across lanes and check the recent transaction history stack (`Stack<Transaction>`).
- **Option 4:** View raw contents of `toll_audit_log.txt`, filter transaction lines, append administrative notes, or clear the log file.
- **Option 5:** Test the JDBC connection, view formatted SQL revenue summaries (`GROUP BY VEHICLE_TYPE`), or run JPA queries to retrieve pass entities.
- **Option 6:** Review global plaza metrics maintained by the `TollManager` singleton.
- **Option 7:** Run the automated end-to-end demo exercising all features in a single automated flow.
- **Option 8:** Reset system statistics and active lane queues, with an optional prompt to re-seed demo vehicles.

---

### 2. Automated End-to-End Test (Option 7)

For a quick and thorough evaluation, choose **Option 7** from the menu.

This runs a preconfigured automated test sequence that exercises all project components in one continuous flow:
1. **Registers 8 test vehicles** spanning all three categories (including edge cases: commercial car, low balance, missing FASTag, overloaded truck, and a government bus).
2. **Distributes vehicles** across 3 lanes using shortest-queue load balancing.
3. **Spawns 3 concurrent `LaneWorker` threads** that process queues simultaneously.
4. **Validates exception handling** as low-balance and inactive-FASTag vehicles trigger `InsufficientBalanceException` and `UnauthorizedVehicleException` without crashing other lanes.
5. **Displays the LIFO transaction history** retrieved from the `Stack`.
6. **Performs JPA bulk insertion** of all completed transactions into the database.
7. **Executes an aggregate JDBC query** to print vehicle counts and revenue totals grouped by category.
8. **Dumps the generated `toll_audit_log.txt`** file to the console to verify file I/O operations.

## ScreenShots


## Refrences
1. Herbert Schildt, Java: The Complete Reference, 11th Edition, Oracle Press / McGraw-Hill
2. Cay S. Horstmann, Core Java Volume I – Fundamentals, 11th Edition, Pearson
3. Oracle Java SE Concurrency Documentation — https://docs.oracle.com/javase/tutorial/essential/concurrency/
4. Hibernate ORM 5.6 Documentation — https://hibernate.org/orm/documentation/5.6/
5. H2 Database Engine Documentation — https://www.h2database.com/

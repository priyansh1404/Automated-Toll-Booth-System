# Project Statement: Automated Toll Booth & Vehicle Management System

---

## 1. Problem Statement

### Operational Challenges in Legacy Toll Booths
Traditional manual toll booths and early-generation electronic toll systems face several practical challenges during day-to-day operations on busy highway networks:

1. **Traffic Congestion from Sequential Processing:** In older systems, vehicles are typically handled one after another in a single-threaded queue. During peak rush hours, this creates severe bottlenecks at toll plazas, leading to long wait times, traffic backlogs, and unnecessary fuel consumption.
2. **Race Conditions in Multi-Lane Financial Records:** When multiple toll lanes operate at the same time and attempt to write to a single centralized ledger or accounting pool without proper synchronization, race conditions occur. These conflicts can cause lost updates, revenue miscalculations, and balance inconsistencies.
3. **Data Loss During Unexpected System Failures:** Toll plazas often lack reliable transactional boundaries. If power drops or the operating system crashes while processing transactions, in-flight vehicle data is often lost without being committed to persistent storage.
4. **Lack of Automated, File-Based Audit Trails:** Many older installations lack local, tamper-evident flat-file logging. When disputes arise or end-of-day audits are required, reconciling differences without structured, timestamped logs is difficult and error-prone.
5. **Inefficient Handling of Payment and Authorization Failures:** If a driver arrives with insufficient balance or an inactive FASTag, manual intervention is often required right at the barrier. Without an automated exception handling mechanism, that entire lane freezes, holding up all following vehicles.

### The Proposed Java Solution
This project addresses these challenges by building a concurrent, fault-tolerant toll management application in Java:
- **Parallel Multi-Lane Processing:** Toll lanes run as independent `Runnable` tasks on separate threads, simulating simultaneous gate operations to prevent plaza bottlenecks.
- **Synchronized Revenue Ledger:** A dedicated `TollRevenueLedger` wraps all financial updates in `synchronized` blocks, ensuring that concurrent payments from different lanes are updated atomically without race conditions.
- **Custom Exception Isolation:** When a vehicle has insufficient wallet balance or an inactive FASTag, the system throws checked exceptions (`InsufficientBalanceException` and `UnauthorizedVehicleException`). These exceptions are caught and handled per vehicle, logging the failure without interrupting the remaining queue in that lane.
- **Polymorphic Toll Derivation:** Vehicle tariffs are calculated dynamically using object-oriented polymorphism, taking into account vehicle type, axle count, weight thresholds, and commercial or government discounts.
- **Dual Persistence Architecture:** Transactions are written to both a persistent audit text file (`toll_audit_log.txt`) via buffered I/O streams and an in-memory relational database using JPA/Hibernate (for structured entity records with accurate tag authorization tracking) and direct JDBC (for fast aggregate reporting).

---

## 2. Scope of the Project

### In-Scope
- **Multi-Threaded Lane Simulation:** Simulating parallel toll gates running on individual threads, with a load-balancing mechanism to enqueue incoming vehicles into the shortest active lane.
- **Polymorphic Vehicle Modeling:** Abstract `Vehicle` class with concrete subclasses (`Car`, `Truck`, `Bus`) overriding `calculateToll()` based on specific business rules.
- **Custom Exception Management:** Domain-specific checked exceptions to isolate payment and tag authorization errors cleanly.
- **In-Memory Data Structures:** Dynamic lane queues using `ArrayList<Vehicle>` and a chronological transaction history stack using `Stack<Transaction>`.
- **Local File Auditing:** Thread-safe append logging, reading, filtering, and clearing of transaction history using `BufferedWriter` and `BufferedReader`.
- **Relational Persistence (JPA & JDBC):**
  - JPA mapping via `VehiclePassEntity` with auto-generated primary keys, accurate FASTag tracking, and JPQL queries.
  - Plain JDBC queries for quick analytical summaries (`COUNT`, `SUM`, `AVG`).
- **Embedded Database:** Zero-setup embedded H2 database (`jdbc:h2:mem:tolldb`) for immediate execution without installing an external database server.
- **Interactive CLI & Automated Test Suite:** Console-based interface for interactive testing (Options 1–6, 8), plus an automated demo mode (Option 7) that executes the complete lifecycle in a single run.

### Out-of-Scope
- **Physical Hardware Interfacing:** No direct integration with physical RFID transceivers, ANPR optical cameras, inductive road sensors, or automatic boom barriers.
- **Live Bank / Payment Gateway APIs:** No real-time connections to external banking switches or the NPCI NETC FASTag clearing house; wallet balances and transactions are simulated in memory and persisted locally.
- **Distributed Cloud Hosting:** The system is designed as a standalone Java application and does not include distributed cloud clustering or container orchestration setups (e.g., Kubernetes).
- **Graphical / Web User Interface:** The application focuses on clean software architecture and concurrency fundamentals using an interactive CLI rather than a web or desktop GUI.

---

## 3. Target Users

1. **Toll Plaza Operators:**
   - Monitor real-time queues across different lanes.
   - Register incoming vehicles and assign them to active lanes.
   - Start concurrent processing sessions and monitor vehicle flow across gates.
   - Clear and reset lane queues between operational shifts (Option 8).

2. **System Administrators & Auditors:**
   - Inspect database records to verify processed passes, tag statuses, and wallet deductions.
   - Generate summary revenue reports using JDBC aggregation queries.
   - Review local file logs (`toll_audit_log.txt`) for reconciliation and dispute resolution.

3. **Academic Evaluators & Reviewers:**
   - Verify how core Java concepts (OOP, Concurrency, Collections, File I/O, JDBC, and JPA/Hibernate) are integrated into a cohesive architecture.
   - Review design decisions like the Template Method pattern, thread synchronization, and checked exception handling.
   - Run the built-in automated test suite (Option 7) for quick verification of all modules.

---

## 4. High-Level Features

- **Dynamic Polymorphic Toll Calculation:**
  Tariff rules are determined at runtime based on vehicle characteristics. Cars pay a flat fee with a commercial surcharge, trucks pay based on axle count and payload weight over a threshold, and buses pay tiered rates based on seating capacity with discounts for government vehicles.

- **Concurrent Multi-Lane Engine:**
  Simulates multiple gates operating simultaneously on separate threads. New vehicles are assigned to the shortest lane to balance traffic, and all lanes process their queues in parallel without blocking one another.

- **Thread-Safe Financial Accounting:**
  A centralized ledger accumulates total revenue and successful pass counts using synchronized critical sections, ensuring clean data consistency even under heavy thread contention.

- **Custom Exception Handling:**
  Checked exceptions isolate issues like insufficient balances or missing FASTags. Vehicles that fail checks are recorded as failed transactions and routed out of the active lane without stopping traffic flow for other motorists.

- **File-Based Audit Logging:**
  Every processed transaction is logged to `toll_audit_log.txt` with pipe-separated fields (transaction ID, registration number, vehicle type, toll charged, balance remaining, lane number, timestamp, and status message), complete with viewing, filtering, event logging, and log resetting capabilities.

- **Dual-Layer Persistence (JPA & JDBC):**
  Completed transactions are persisted to an embedded H2 database as `VehiclePassEntity` instances using JPA's `EntityManager` (accurately recording toll, balance, pass outcome, and tag authorization). In addition, native JDBC queries are provided to calculate revenue summaries and category-wise statistics efficiently.

- **Automated End-to-End Demo Mode:**
  Menu Option 7 triggers a complete automated test sequence that registers sample vehicles, load-balances them across lanes, executes multi-threaded processing, handles exception cases, updates the database, and displays file logs without requiring manual input.
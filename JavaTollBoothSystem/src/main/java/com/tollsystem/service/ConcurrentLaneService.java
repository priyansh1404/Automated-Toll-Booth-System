package com.tollsystem.service;

import com.tollsystem.model.Transaction;
import com.tollsystem.model.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Service that orchestrates multi-lane concurrent toll processing.
 *
 * Unit 4 data structures:
 *   - ArrayList<Vehicle> : dynamic lane queue per lane worker.
 *   - Stack<Transaction> : LIFO transaction history for auditing.
 */
public class ConcurrentLaneService {

    public static final int MAX_LANES = 6;

    private final List<ArrayList<Vehicle>> laneQueues;
    private final Stack<Transaction>       transactionHistory;
    private final TollRevenueLedger        revenueLedger;
    private final AuditLoggerService       auditLogger;
    private final int                      activeLaneCount;

    /**
     * Constructs the concurrent lane service.
     *
     * @param activeLaneCount number of lanes to activate (1 to MAX_LANES)
     * @param auditLogger     the audit logger service
     */
    public ConcurrentLaneService(int activeLaneCount, AuditLoggerService auditLogger) {
        if (activeLaneCount < 1 || activeLaneCount > MAX_LANES) {
            throw new IllegalArgumentException(
                    "activeLaneCount must be between 1 and " + MAX_LANES);
        }
        this.activeLaneCount    = activeLaneCount;
        this.auditLogger        = auditLogger;
        this.revenueLedger      = new TollRevenueLedger();
        this.transactionHistory = new Stack<>();
        this.laneQueues         = new ArrayList<>();

        for (int i = 0; i < activeLaneCount; i++) {
            laneQueues.add(new ArrayList<>());
        }
    }

    /**
     * Adds a vehicle to a specific lane queue.
     *
     * @param laneIndex zero-based lane index (0 to activeLaneCount - 1)
     * @param vehicle   the vehicle to enqueue
     */
    public void addVehicleToLane(int laneIndex, Vehicle vehicle) {
        if (laneIndex < 0 || laneIndex >= activeLaneCount) {
            throw new IndexOutOfBoundsException(
                    "Invalid lane index: " + laneIndex + ". Active lanes: 0-" + (activeLaneCount - 1));
        }
        laneQueues.get(laneIndex).add(vehicle);
    }

    /**
     * Adds a vehicle to the lane with the fewest queued vehicles (load-balancing).
     *
     * @param vehicle the vehicle to add
     */
    public void addVehicleToShortestLane(Vehicle vehicle) {
        int shortestLaneIdx = 0;
        int minSize = laneQueues.get(0).size();
        for (int i = 1; i < activeLaneCount; i++) {
            if (laneQueues.get(i).size() < minSize) {
                minSize = laneQueues.get(i).size();
                shortestLaneIdx = i;
            }
        }
        laneQueues.get(shortestLaneIdx).add(vehicle);
        System.out.printf("  Vehicle %s assigned to Lane-%d (queue size: %d)%n",
                vehicle.getRegistrationNum(), shortestLaneIdx + 1, minSize + 1);
    }

    /**
     * Launches one thread per active lane, processes all queued vehicles
     * concurrently, then blocks until all threads have completed.
     *
     * @return the session revenue ledger after processing
     */
    public TollRevenueLedger processAllLanesConcurrently() {
        System.out.println("\n=== Starting Concurrent Multi-Lane Toll Processing ===");
        System.out.printf("Active Lanes: %d | Total Vehicles: %d%n",
                activeLaneCount, getTotalQueuedVehicles());

        List<Transaction> transactionSink =
                Collections.synchronizedList(new ArrayList<>());

        List<Thread> laneThreads = new ArrayList<>();
        for (int i = 0; i < activeLaneCount; i++) {
            LaneWorker worker = new LaneWorker(
                    i + 1,
                    laneQueues.get(i),
                    revenueLedger,
                    transactionSink,
                    auditLogger);
            Thread thread = new Thread(worker, "LaneThread-" + (i + 1));
            laneThreads.add(thread);
        }

        for (Thread t : laneThreads) {
            t.start();
        }

        for (Thread t : laneThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main thread interrupted while waiting for lanes.");
            }
        }

        for (Transaction txn : transactionSink) {
            transactionHistory.push(txn);
        }

        // Vehicles have passed through the toll plaza gates; clear waiting queues
        for (ArrayList<Vehicle> queue : laneQueues) {
            queue.clear();
        }

        System.out.println("\n=== All Lanes Finished Processing ===");
        System.out.println(revenueLedger);
        return revenueLedger;
    }

    /**
     * Clears all waiting vehicles from every lane queue.
     */
    public synchronized void clearQueues() {
        for (ArrayList<Vehicle> queue : laneQueues) {
            queue.clear();
        }
    }

    /**
     * Resets the entire lane service: clears queues, clears transaction history, and resets ledger.
     */
    public synchronized void resetAll() {
        clearQueues();
        transactionHistory.clear();
        revenueLedger.reset();
    }

    /**
     * Peeks at the most recent transaction (top of stack) without removing it.
     *
     * @return the most recent Transaction, or null if empty
     */
    public Transaction peekLatestTransaction() {
        return transactionHistory.isEmpty() ? null : transactionHistory.peek();
    }

    /**
     * Pops and returns the most recent transaction from the history stack.
     */
    public Transaction popLatestTransaction() {
        return transactionHistory.pop();
    }

    /**
     * Returns the full transaction history stack.
     */
    public Stack<Transaction> getTransactionHistory() {
        return transactionHistory;
    }

    /**
     * Returns a snapshot of vehicles queued in the specified lane.
     */
    public List<Vehicle> getLaneQueueSnapshot(int laneIndex) {
        return Collections.unmodifiableList(laneQueues.get(laneIndex));
    }

    /** Returns the total number of vehicles queued across all lanes. */
    public int getTotalQueuedVehicles() {
        return laneQueues.stream().mapToInt(ArrayList::size).sum();
    }

    public int              getActiveLaneCount() { return activeLaneCount; }
    public TollRevenueLedger getRevenueLedger()  { return revenueLedger; }

    /** Prints a formatted display of all lane queues to stdout. */
    public void displayAllLaneQueues() {
        System.out.println("\n--- Current Lane Queue Status ---");
        for (int i = 0; i < activeLaneCount; i++) {
            ArrayList<Vehicle> queue = laneQueues.get(i);
            System.out.printf("  Lane-%d (%d vehicle(s)):%n", i + 1, queue.size());
            if (queue.isEmpty()) {
                System.out.println("    [empty]");
            } else {
                for (Vehicle v : queue) {
                    System.out.println("    " + v);
                }
            }
        }
        System.out.println("---------------------------------");
    }

    /** Prints the transaction history stack from most-recent to oldest. */
    public void displayTransactionHistory() {
        System.out.println("\n--- Transaction History (Most Recent First) ---");
        if (transactionHistory.isEmpty()) {
            System.out.println("  [No transactions yet]");
        } else {
            for (int i = transactionHistory.size() - 1; i >= 0; i--) {
                System.out.println("  " + transactionHistory.get(i));
            }
        }
        System.out.println("----------------------------------------------");
    }
}
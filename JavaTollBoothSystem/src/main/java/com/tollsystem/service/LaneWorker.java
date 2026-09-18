package com.tollsystem.service;

import com.tollsystem.exception.InsufficientBalanceException;
import com.tollsystem.exception.UnauthorizedVehicleException;
import com.tollsystem.model.Transaction;
import com.tollsystem.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * Runnable worker that simulates a single toll-gate lane operating concurrently.
 *
 * Each LaneWorker holds a local queue of vehicles (modelled as an ArrayList)
 * and processes them sequentially. Revenue updates are made on the shared
 * TollRevenueLedger inside a synchronized block to prevent race conditions
 * when multiple lanes run simultaneously.
 */
public class LaneWorker implements Runnable {

    private final int              laneNumber;
    private final ArrayList<Vehicle> laneQueue;
    private final TollRevenueLedger  revenueLedger;
    private final TollManager        tollManager;
    private final List<Transaction>  transactionSink;
    private final AuditLoggerService auditLogger;

    /**
     * Constructs a LaneWorker.
     *
     * @param laneNumber      unique lane identifier
     * @param vehicles        initial vehicles queued for this lane
     * @param revenueLedger   shared revenue ledger (synchronized)
     * @param transactionSink shared list to collect completed transactions
     * @param auditLogger     service for writing audit log entries
     */
    public LaneWorker(int laneNumber, List<Vehicle> vehicles,
                      TollRevenueLedger revenueLedger,
                      List<Transaction> transactionSink,
                      AuditLoggerService auditLogger) {
        this.laneNumber      = laneNumber;
        this.laneQueue       = new ArrayList<>(vehicles);
        this.revenueLedger   = revenueLedger;
        this.tollManager     = TollManager.getInstance();
        this.transactionSink = transactionSink;
        this.auditLogger     = auditLogger;
    }

    /**
     * Processes every vehicle in this lane queue sequentially.
     * Revenue is updated on the shared ledger inside a synchronized block
     * to guarantee mutual exclusion.
     */
    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        System.out.printf("%n[Lane-%d | %s] Starting -- %d vehicle(s) in queue.%n",
                laneNumber, threadName, laneQueue.size());

        for (Vehicle vehicle : laneQueue) {
            long txnId = tollManager.generateTransactionId();
            try {
                // Authorization check -- vehicle must have FASTag enabled
                if (!vehicle.isFastagEnabled()) {
                    throw new UnauthorizedVehicleException(
                            vehicle.getRegistrationNum(), "NO_FASTAG");
                }

                // Balance check before deduction
                double toll = vehicle.calculateToll();
                if (vehicle.getWalletBalance() < toll) {
                    throw new InsufficientBalanceException(
                            vehicle.getRegistrationNum(),
                            vehicle.getWalletBalance(),
                            toll);
                }

                // Deduct toll from vehicle wallet
                double deducted = vehicle.processPayment();

                // Update shared revenue ledger (SYNCHRONIZED -- critical section)
                revenueLedger.addRevenue(deducted);

                // Update global statistics via TollManager
                tollManager.recordSuccessfulTransaction(deducted, vehicle.getVehicleType());

                // Build successful transaction record
                Transaction txn = new Transaction(
                        txnId,
                        vehicle.getRegistrationNum(),
                        vehicle.getVehicleType(),
                        deducted,
                        vehicle.getWalletBalance(),
                        laneNumber,
                        true,
                        "Toll collected successfully",
                        vehicle.isFastagEnabled());

                synchronized (transactionSink) {
                    transactionSink.add(txn);
                }

                auditLogger.logTransaction(txn);

                System.out.printf("[Lane-%d] OK Processed %s | Toll: INR %.2f | Remaining: INR %.2f%n",
                        laneNumber, vehicle.getRegistrationNum(),
                        deducted, vehicle.getWalletBalance());

                Thread.sleep(200);

            } catch (InsufficientBalanceException e) {
                tollManager.recordFailedTransaction();
                Transaction txn = new Transaction(
                        txnId,
                        vehicle.getRegistrationNum(),
                        vehicle.getVehicleType(),
                        0.0,
                        vehicle.getWalletBalance(),
                        laneNumber,
                        false,
                        "FAILED: " + e.getMessage(),
                        vehicle.isFastagEnabled());
                synchronized (transactionSink) {
                    transactionSink.add(txn);
                }
                auditLogger.logTransaction(txn);
                System.out.printf("[Lane-%d] FAIL InsufficientBalance %s: %s%n",
                        laneNumber, vehicle.getRegistrationNum(), e.getMessage());

            } catch (UnauthorizedVehicleException e) {
                tollManager.recordFailedTransaction();
                Transaction txn = new Transaction(
                        txnId,
                        vehicle.getRegistrationNum(),
                        vehicle.getVehicleType(),
                        0.0,
                        vehicle.getWalletBalance(),
                        laneNumber,
                        false,
                        "FAILED: " + e.getMessage(),
                        vehicle.isFastagEnabled());
                synchronized (transactionSink) {
                    transactionSink.add(txn);
                }
                auditLogger.logTransaction(txn);
                System.out.printf("[Lane-%d] FAIL Unauthorized %s: %s%n",
                        laneNumber, vehicle.getRegistrationNum(), e.getMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.printf("[Lane-%d] Lane interrupted during processing.%n", laneNumber);
                break;
            } catch (Exception e) {
                tollManager.recordFailedTransaction();
                Transaction txn = new Transaction(
                        txnId,
                        vehicle.getRegistrationNum(),
                        vehicle.getVehicleType(),
                        0.0,
                        vehicle.getWalletBalance(),
                        laneNumber,
                        false,
                        "ERROR: " + e.getMessage(),
                        vehicle.isFastagEnabled());
                synchronized (transactionSink) {
                    transactionSink.add(txn);
                }
                auditLogger.logTransaction(txn);
                System.err.printf("[Lane-%d] ERROR processing %s: %s%n",
                        laneNumber, vehicle.getRegistrationNum(), e.getMessage());
            }
        }

        System.out.printf("[Lane-%d | %s] Finished processing.%n", laneNumber, threadName);
    }

    /** Returns the current snapshot of this lane vehicle queue. */
    public ArrayList<Vehicle> getLaneQueue() {
        return new ArrayList<>(laneQueue);
    }

    public int getLaneNumber() {
        return laneNumber;
    }
}
package com.tollsystem.service;

import com.tollsystem.model.Vehicle;
import com.tollsystem.model.VehicleType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton service that maintains global statistics for the entire toll system.
 *
 * Design Pattern: Classic thread-safe Singleton using double-checked locking (DCL)
 * with a volatile instance field to prevent partially-initialised objects from
 * being visible to other threads.
 */
public class TollManager {

    private static volatile TollManager instance;

    private double totalRevenueCollected   = 0.0;
    private int    totalVehiclesProcessed  = 0;
    private int    totalFailedTransactions = 0;

    private final Map<VehicleType, Integer> vehicleTypeCount =
            Collections.synchronizedMap(new HashMap<>());

    private final AtomicLong transactionIdCounter = new AtomicLong(1000L);

    private final Map<String, Vehicle> vehicleRegistry =
            Collections.synchronizedMap(new HashMap<>());

    private TollManager() {
        for (VehicleType type : VehicleType.values()) {
            vehicleTypeCount.put(type, 0);
        }
    }

    /**
     * Returns the single, shared instance of TollManager.
     * Uses double-checked locking for thread safety.
     *
     * @return the singleton instance
     */
    public static TollManager getInstance() {
        if (instance == null) {
            synchronized (TollManager.class) {
                if (instance == null) {
                    instance = new TollManager();
                }
            }
        }
        return instance;
    }

    /**
     * Registers a vehicle in the system registry.
     *
     * @param vehicle the vehicle to register
     * @return true if registration was successful; false if already present
     */
    public boolean registerVehicle(Vehicle vehicle) {
        if (vehicleRegistry.containsKey(vehicle.getRegistrationNum())) {
            return false;
        }
        vehicleRegistry.put(vehicle.getRegistrationNum(), vehicle);
        return true;
    }

    /**
     * Looks up a registered vehicle by its registration number.
     *
     * @param registrationNum the vehicle registration plate
     * @return the Vehicle, or null if not found
     */
    public Vehicle findVehicle(String registrationNum) {
        return vehicleRegistry.get(registrationNum);
    }

    /**
     * Returns a read-only view of the vehicle registry.
     */
    public Map<String, Vehicle> getVehicleRegistry() {
        return Collections.unmodifiableMap(vehicleRegistry);
    }

    /**
     * Generates the next unique transaction ID. Thread-safe via AtomicLong.
     *
     * @return next transaction ID
     */
    public long generateTransactionId() {
        return transactionIdCounter.getAndIncrement();
    }

    /**
     * Records a successful toll collection event.
     *
     * @param amount      toll amount collected (INR)
     * @param vehicleType the type of vehicle processed
     */
    public synchronized void recordSuccessfulTransaction(double amount,
                                                          VehicleType vehicleType) {
        totalRevenueCollected  += amount;
        totalVehiclesProcessed += 1;
        vehicleTypeCount.merge(vehicleType, 1, Integer::sum);
    }

    /** Records a failed transaction event. */
    public synchronized void recordFailedTransaction() {
        totalFailedTransactions += 1;
    }

    public synchronized double getTotalRevenueCollected()    { return totalRevenueCollected; }
    public synchronized int    getTotalVehiclesProcessed()   { return totalVehiclesProcessed; }
    public synchronized int    getTotalFailedTransactions()  { return totalFailedTransactions; }

    public Map<VehicleType, Integer> getVehicleTypeCount() {
        return Collections.unmodifiableMap(vehicleTypeCount);
    }

    /**
     * Resets all global statistics. Does NOT clear the vehicle registry.
     */
    public synchronized void resetStatistics() {
        totalRevenueCollected   = 0.0;
        totalVehiclesProcessed  = 0;
        totalFailedTransactions = 0;
        for (VehicleType type : VehicleType.values()) {
            vehicleTypeCount.put(type, 0);
        }
    }

    /**
     * Clears the vehicle registry.
     */
    public synchronized void clearVehicleRegistry() {
        vehicleRegistry.clear();
    }

    /**
     * Resets all statistics and clears the vehicle registry.
     */
    public synchronized void resetAll() {
        resetStatistics();
        clearVehicleRegistry();
    }

    /**
     * Generates a formatted summary of global toll statistics.
     *
     * @return multi-line summary string
     */
    public synchronized String generateSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n============================================================\n");
        sb.append("         TOLL SYSTEM -- GLOBAL STATISTICS REPORT\n");
        sb.append("============================================================\n");
        sb.append(String.format("  Total Revenue Collected : INR %.2f%n", totalRevenueCollected));
        sb.append(String.format("  Total Vehicles Processed: %d%n",       totalVehiclesProcessed));
        sb.append(String.format("  Failed Transactions     : %d%n",       totalFailedTransactions));
        sb.append(String.format("  Registered Vehicles     : %d%n",       vehicleRegistry.size()));
        sb.append("\n  Vehicle Type Breakdown:\n");
        vehicleTypeCount.forEach((type, count) ->
                sb.append(String.format("    %-10s : %d%n", type, count)));
        sb.append("============================================================\n");
        return sb.toString();
    }
}
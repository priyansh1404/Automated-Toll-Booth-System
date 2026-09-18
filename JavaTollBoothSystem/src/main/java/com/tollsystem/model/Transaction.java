package com.tollsystem.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable value object representing a single completed toll transaction.
 * Instances are pushed onto the transaction-history Stack maintained by ConcurrentLaneService.
 */
public final class Transaction {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final long          transactionId;
    private final String        registrationNum;
    private final VehicleType   vehicleType;
    private final double        tollAmount;
    private final double        remainingBalance;
    private final int           laneNumber;
    private final LocalDateTime timestamp;
    private final boolean       successful;
    private final String        statusMessage;
    private final boolean       fastagEnabled;

    /**
     * Full constructor for a completed (successful or failed) transaction.
     *
     * @param transactionId    unique ID
     * @param registrationNum  vehicle registration plate
     * @param vehicleType      vehicle category
     * @param tollAmount       amount charged (INR)
     * @param remainingBalance wallet balance post-deduction (INR)
     * @param laneNumber       toll lane number
     * @param successful       true if toll was collected successfully
     * @param statusMessage    human-readable status or error description
     * @param fastagEnabled    true if FASTag was active
     */
    public Transaction(long transactionId, String registrationNum,
                       VehicleType vehicleType, double tollAmount,
                       double remainingBalance, int laneNumber,
                       boolean successful, String statusMessage,
                       boolean fastagEnabled) {
        this.transactionId    = transactionId;
        this.registrationNum  = registrationNum;
        this.vehicleType      = vehicleType;
        this.tollAmount       = tollAmount;
        this.remainingBalance = remainingBalance;
        this.laneNumber       = laneNumber;
        this.timestamp        = LocalDateTime.now();
        this.successful       = successful;
        this.statusMessage    = statusMessage;
        this.fastagEnabled    = fastagEnabled;
    }

    /**
     * Backward-compatible constructor defaulting fastagEnabled to true.
     */
    public Transaction(long transactionId, String registrationNum,
                       VehicleType vehicleType, double tollAmount,
                       double remainingBalance, int laneNumber,
                       boolean successful, String statusMessage) {
        this(transactionId, registrationNum, vehicleType, tollAmount,
             remainingBalance, laneNumber, successful, statusMessage, true);
    }

    public long          getTransactionId()    { return transactionId; }
    public String        getRegistrationNum()  { return registrationNum; }
    public VehicleType   getVehicleType()      { return vehicleType; }
    public double        getTollAmount()       { return tollAmount; }
    public double        getRemainingBalance() { return remainingBalance; }
    public int           getLaneNumber()       { return laneNumber; }
    public LocalDateTime getTimestamp()        { return timestamp; }
    public boolean       isSuccessful()        { return successful; }
    public String        getStatusMessage()    { return statusMessage; }
    public boolean       isFastagEnabled()     { return fastagEnabled; }

    /**
     * Returns a pipe-delimited string suitable for writing to the audit log file.
     */
    public String toAuditLogLine() {
        return String.format("%d|%s|%s|%.2f|%.2f|%d|%s|%s|%s",
                transactionId, registrationNum, vehicleType,
                tollAmount, remainingBalance, laneNumber,
                timestamp.format(FORMATTER),
                successful ? "SUCCESS" : "FAILED",
                statusMessage);
    }

    @Override
    public String toString() {
        return String.format(
                "TxnID: %d | Vehicle: %s (%s) | Lane: %d | Toll: INR %.2f | " +
                "Balance After: INR %.2f | %s | %s | %s",
                transactionId, registrationNum, vehicleType, laneNumber,
                tollAmount, remainingBalance,
                timestamp.format(FORMATTER),
                successful ? "SUCCESS" : "FAILED",
                statusMessage);
    }
}
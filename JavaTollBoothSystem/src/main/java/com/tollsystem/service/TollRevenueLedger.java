package com.tollsystem.service;

/**
 * Thread-safe shared ledger that accumulates toll revenue collected across
 * all concurrent lane workers.
 *
 * All mutating operations are declared synchronized to guarantee
 * that no two threads can simultaneously update the revenue total,
 * preventing race conditions and ensuring data integrity.
 */
public class TollRevenueLedger {

    private double sessionRevenue      = 0.0;
    private int    successfulDeductions = 0;

    /**
     * Adds the given amount to the session revenue total.
     * Synchronized to prevent race conditions when called from multiple lane
     * threads simultaneously.
     *
     * @param amount toll amount to add (INR); must be >= 0
     */
    public synchronized void addRevenue(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Revenue amount cannot be negative.");
        }
        this.sessionRevenue       += amount;
        this.successfulDeductions += 1;
    }

    /**
     * Returns the total session revenue accumulated so far.
     * Synchronized to guarantee a consistent read across threads.
     */
    public synchronized double getSessionRevenue()     { return sessionRevenue; }

    /** Returns the count of successful toll deductions recorded. */
    public synchronized int getSuccessfulDeductions()  { return successfulDeductions; }

    /** Resets the session ledger to zero. */
    public synchronized void reset() {
        this.sessionRevenue       = 0.0;
        this.successfulDeductions = 0;
    }

    @Override
    public synchronized String toString() {
        return String.format("TollRevenueLedger{sessionRevenue=INR %.2f, deductions=%d}",
                sessionRevenue, successfulDeductions);
    }
}
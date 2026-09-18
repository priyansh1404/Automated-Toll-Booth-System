package com.tollsystem.exception;

/**
 * Checked exception thrown when a vehicle FASTag wallet balance is
 * insufficient to cover the calculated toll amount.
 */
public class InsufficientBalanceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String registrationNum;
    private final double availableBalance;
    private final double requiredAmount;

    /**
     * Constructs the exception with full context about the shortfall.
     *
     * @param registrationNum  vehicle registration plate
     * @param availableBalance wallet balance at the time of failure (INR)
     * @param requiredAmount   toll amount that could not be deducted (INR)
     */
    public InsufficientBalanceException(String registrationNum,
                                        double availableBalance,
                                        double requiredAmount) {
        super(String.format(
                "Insufficient FASTag balance for vehicle [%s]. " +
                "Available: INR %.2f, Required: INR %.2f, Shortfall: INR %.2f.",
                registrationNum, availableBalance, requiredAmount,
                (requiredAmount - availableBalance)));
        this.registrationNum  = registrationNum;
        this.availableBalance = availableBalance;
        this.requiredAmount   = requiredAmount;
    }

    /**
     * Constructs the exception with a custom message plus cause.
     *
     * @param message  human-readable description of the failure
     * @param cause    the underlying cause, if any
     */
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
        this.registrationNum  = "UNKNOWN";
        this.availableBalance = 0.0;
        this.requiredAmount   = 0.0;
    }

    public String getRegistrationNum()  { return registrationNum; }
    public double getAvailableBalance() { return availableBalance; }
    public double getRequiredAmount()   { return requiredAmount; }

    /** Convenience method to compute the shortfall amount. */
    public double getShortfall() {
        return requiredAmount - availableBalance;
    }
}
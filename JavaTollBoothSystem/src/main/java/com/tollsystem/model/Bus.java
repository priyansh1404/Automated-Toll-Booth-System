package com.tollsystem.model;

/**
 * Concrete vehicle representing a passenger bus.
 *
 * Toll Strategy: Capacity-based calculation with tiered rates:
 *   Mini-bus  (<= 20 seats)  : INR 100.00 flat
 *   Standard  (21-45 seats)  : INR 200.00 flat
 *   Large bus (> 45 seats)   : INR 300.00 flat
 *   Government buses receive a 25% discount.
 */
public class Bus extends Vehicle {

    public static final double MINI_BUS_TOLL      = 100.0;
    public static final double STANDARD_BUS_TOLL  = 200.0;
    public static final double LARGE_BUS_TOLL     = 300.0;
    public static final double GOVT_DISCOUNT_FACTOR = 0.75;

    private int     seatingCapacity;
    private boolean governmentBus;

    /**
     * Constructs a Bus with all required attributes.
     *
     * @param registrationNum  unique registration plate string
     * @param walletBalance    initial FASTag wallet balance (INR)
     * @param fastagEnabled    true if FASTag is activated
     * @param seatingCapacity  number of passenger seats (must be >= 1)
     * @param governmentBus    true for government/state-transport buses
     */
    public Bus(String registrationNum, double walletBalance,
               boolean fastagEnabled, int seatingCapacity, boolean governmentBus) {
        super(registrationNum, walletBalance, fastagEnabled, VehicleType.BUS);
        if (seatingCapacity < 1) {
            throw new IllegalArgumentException("Seating capacity must be at least 1.");
        }
        this.seatingCapacity = seatingCapacity;
        this.governmentBus   = governmentBus;
    }

    /**
     * Returns the capacity-tiered toll, applying a government discount if applicable.
     *
     * @return toll amount in INR
     */
    @Override
    public double calculateToll() {
        double baseToll;
        if (seatingCapacity <= 20) {
            baseToll = MINI_BUS_TOLL;
        } else if (seatingCapacity <= 45) {
            baseToll = STANDARD_BUS_TOLL;
        } else {
            baseToll = LARGE_BUS_TOLL;
        }
        return governmentBus ? baseToll * GOVT_DISCOUNT_FACTOR : baseToll;
    }

    public int getSeatingCapacity()                      { return seatingCapacity; }
    public void setSeatingCapacity(int n) {
        if (n < 1) throw new IllegalArgumentException("Seating capacity must be at least 1.");
        this.seatingCapacity = n;
    }
    public boolean isGovernmentBus()                     { return governmentBus; }
    public void setGovernmentBus(boolean g)              { this.governmentBus = g; }

    @Override
    public String toString() {
        return super.toString() + String.format(
                " | Seats: %d | %s | Toll: INR %.2f",
                seatingCapacity,
                governmentBus ? "Govt Bus" : "Private Bus",
                calculateToll());
    }
}
package com.tollsystem.model;

/**
 * Concrete vehicle representing a heavy goods truck.
 *
 * Toll Strategy: Weight- and axle-based calculation.
 * Formula:
 *   toll = BASE_RATE_PER_AXLE * numberOfAxles
 *          + (weightInTonnes > WEIGHT_THRESHOLD
 *             ? (weightInTonnes - WEIGHT_THRESHOLD) * OVERLOAD_SURCHARGE_PER_TONNE
 *             : 0)
 */
public class Truck extends Vehicle {

    public static final double BASE_RATE_PER_AXLE           = 150.0;
    public static final double WEIGHT_THRESHOLD              = 10.0;
    public static final double OVERLOAD_SURCHARGE_PER_TONNE  = 50.0;

    private double weightInTonnes;
    private int    numberOfAxles;

    /**
     * Constructs a Truck with all required attributes.
     *
     * @param registrationNum unique registration plate string
     * @param walletBalance   initial FASTag wallet balance (INR)
     * @param fastagEnabled   true if FASTag is activated
     * @param weightInTonnes  payload weight in metric tonnes
     * @param numberOfAxles   number of axles (must be >= 2)
     */
    public Truck(String registrationNum, double walletBalance,
                 boolean fastagEnabled, double weightInTonnes, int numberOfAxles) {
        super(registrationNum, walletBalance, fastagEnabled, VehicleType.TRUCK);
        if (numberOfAxles < 2) {
            throw new IllegalArgumentException("A truck must have at least 2 axles.");
        }
        this.weightInTonnes = weightInTonnes;
        this.numberOfAxles  = numberOfAxles;
    }

    /**
     * Calculates the toll based on axle count and weight.
     *
     * @return toll amount in INR
     */
    @Override
    public double calculateToll() {
        double axleToll = BASE_RATE_PER_AXLE * numberOfAxles;
        double overloadSurcharge = 0.0;
        if (weightInTonnes > WEIGHT_THRESHOLD) {
            overloadSurcharge = (weightInTonnes - WEIGHT_THRESHOLD) * OVERLOAD_SURCHARGE_PER_TONNE;
        }
        return axleToll + overloadSurcharge;
    }

    public double getWeightInTonnes()              { return weightInTonnes; }
    public void setWeightInTonnes(double w)        { this.weightInTonnes = w; }
    public int getNumberOfAxles()                  { return numberOfAxles; }
    public void setNumberOfAxles(int n) {
        if (n < 2) throw new IllegalArgumentException("A truck must have at least 2 axles.");
        this.numberOfAxles = n;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(
                " | Axles: %d | Weight: %.1f T | Toll: INR %.2f",
                numberOfAxles, weightInTonnes, calculateToll());
    }
}
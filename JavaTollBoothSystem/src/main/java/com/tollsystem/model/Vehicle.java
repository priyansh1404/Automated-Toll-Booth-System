package com.tollsystem.model;

/**
 * Abstract base class representing a generic vehicle in the toll system.
 *
 * All concrete vehicle types must extend this class and provide an
 * implementation of calculateToll(), which encodes the business
 * rules for how the toll amount is derived for that vehicle category.
 *
 * Design Pattern: This class participates in the Template Method pattern --
 * processPayment() defines the skeleton algorithm while calculateToll() is
 * deferred to subclasses.
 */
public abstract class Vehicle {

    private String registrationNum;
    private double walletBalance;
    private boolean fastagEnabled;
    private VehicleType vehicleType;

    /**
     * Constructs a Vehicle with all required attributes.
     *
     * @param registrationNum unique registration plate string
     * @param walletBalance   initial FASTag wallet balance (INR)
     * @param fastagEnabled   true if FASTag is activated
     * @param vehicleType     the category of this vehicle
     */
    public Vehicle(String registrationNum, double walletBalance,
                   boolean fastagEnabled, VehicleType vehicleType) {
        this.registrationNum = registrationNum;
        this.walletBalance   = walletBalance;
        this.fastagEnabled   = fastagEnabled;
        this.vehicleType     = vehicleType;
    }

    /**
     * Calculates the toll amount applicable to this vehicle.
     * Subclasses implement distinct strategies: Car (flat rate),
     * Truck (weight/axle-based), Bus (capacity-based).
     *
     * @return toll amount in INR (always >= 0)
     */
    public abstract double calculateToll();

    /**
     * Template method: deducts the calculated toll from the wallet balance.
     * Subclasses do NOT override this; they only override calculateToll().
     *
     * @return the toll amount that was deducted
     * @throws IllegalStateException if FASTag is not enabled
     */
    public double processPayment() {
        if (!fastagEnabled) {
            throw new IllegalStateException(
                "FASTag not enabled for vehicle: " + registrationNum);
        }
        double toll = calculateToll();
        this.walletBalance -= toll;
        return toll;
    }

    public String getRegistrationNum()                         { return registrationNum; }
    public void setRegistrationNum(String registrationNum)     { this.registrationNum = registrationNum; }
    public double getWalletBalance()                           { return walletBalance; }
    public void setWalletBalance(double walletBalance)         { this.walletBalance = walletBalance; }
    public boolean isFastagEnabled()                           { return fastagEnabled; }
    public void setFastagEnabled(boolean fastagEnabled)        { this.fastagEnabled = fastagEnabled; }
    public VehicleType getVehicleType()                        { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType)        { this.vehicleType = vehicleType; }

    @Override
    public String toString() {
        return String.format("[%s | RegNo: %s | Balance: INR %.2f | FASTag: %s]",
                vehicleType, registrationNum, walletBalance,
                fastagEnabled ? "Active" : "Inactive");
    }
}
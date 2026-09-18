package com.tollsystem.model;

/**
 * Concrete vehicle representing a passenger car.
 *
 * Toll Strategy: Flat-rate toll regardless of time of day or route.
 * A surcharge multiplier is applied when the car is commercial/taxi.
 *
 * Base toll rate: INR 80.00 per pass.
 */
public class Car extends Vehicle {

    public static final double BASE_TOLL_RATE       = 80.0;
    public static final double COMMERCIAL_MULTIPLIER = 1.5;

    private boolean commercial;

    /**
     * Constructs a Car with the provided attributes.
     *
     * @param registrationNum unique registration plate string
     * @param walletBalance   initial FASTag wallet balance (INR)
     * @param fastagEnabled   true if FASTag is activated
     * @param commercial      true for commercial/taxi cars
     */
    public Car(String registrationNum, double walletBalance,
               boolean fastagEnabled, boolean commercial) {
        super(registrationNum, walletBalance, fastagEnabled, VehicleType.CAR);
        this.commercial = commercial;
    }

    /**
     * Returns the applicable toll for this car.
     * Standard private car : INR 80.00
     * Commercial / taxi car: INR 120.00
     *
     * @return toll amount in INR
     */
    @Override
    public double calculateToll() {
        return commercial ? BASE_TOLL_RATE * COMMERCIAL_MULTIPLIER : BASE_TOLL_RATE;
    }

    public boolean isCommercial()                  { return commercial; }
    public void setCommercial(boolean commercial)  { this.commercial = commercial; }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Type: %s Car | Toll: INR %.2f",
                commercial ? "Commercial" : "Private", calculateToll());
    }
}
package com.tollsystem.model;

/**
 * Enumeration representing the supported vehicle categories in the toll system.
 * Each type maps to a distinct toll-calculation strategy in the concrete Vehicle subclasses.
 */
public enum VehicleType {
    /** Passenger cars -- flat-rate toll. */
    CAR,
    /** Heavy goods trucks -- weight/axle-based toll. */
    TRUCK,
    /** Passenger buses -- capacity-based toll. */
    BUS
}
package com.tollsystem.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a single vehicle pass record persisted in the database.
 * Maps to the VEHICLE_PASS table.
 */
@Entity
@Table(name = "VEHICLE_PASS")
public class VehiclePassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PASS_ID", nullable = false, updatable = false)
    private Long passId;

    @Column(name = "REGISTRATION_NUM", nullable = false, length = 20)
    private String registrationNum;

    @Column(name = "VEHICLE_TYPE", nullable = false, length = 10)
    private String vehicleType;

    @Column(name = "TOLL_AMOUNT", nullable = false)
    private double tollAmount;

    @Column(name = "REMAINING_BALANCE", nullable = false)
    private double remainingBalance;

    @Column(name = "LANE_NUMBER", nullable = false)
    private int laneNumber;

    @Column(name = "PASS_TIMESTAMP", nullable = false)
    private LocalDateTime passTimestamp;

    @Column(name = "STATUS", nullable = false, length = 10)
    private String status;

    @Column(name = "STATUS_MESSAGE", length = 500)
    private String statusMessage;

    @Column(name = "FASTAG_ENABLED", nullable = false)
    private boolean fastagEnabled;

    /** JPA requires a no-argument constructor. */
    public VehiclePassEntity() {}

    /** Full constructor. */
    public VehiclePassEntity(String registrationNum, String vehicleType,
                              double tollAmount, double remainingBalance,
                              int laneNumber, LocalDateTime passTimestamp,
                              String status, String statusMessage,
                              boolean fastagEnabled) {
        this.registrationNum  = registrationNum;
        this.vehicleType      = vehicleType;
        this.tollAmount       = tollAmount;
        this.remainingBalance = remainingBalance;
        this.laneNumber       = laneNumber;
        this.passTimestamp    = passTimestamp;
        this.status           = status;
        this.statusMessage    = statusMessage;
        this.fastagEnabled    = fastagEnabled;
    }

    public Long getPassId()               { return passId; }
    public void setPassId(Long passId)    { this.passId = passId; }

    public String getRegistrationNum()                        { return registrationNum; }
    public void setRegistrationNum(String registrationNum)    { this.registrationNum = registrationNum; }

    public String getVehicleType()                 { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public double getTollAmount()              { return tollAmount; }
    public void setTollAmount(double tollAmount) { this.tollAmount = tollAmount; }

    public double getRemainingBalance()                    { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance){ this.remainingBalance = remainingBalance; }

    public int getLaneNumber()               { return laneNumber; }
    public void setLaneNumber(int laneNumber){ this.laneNumber = laneNumber; }

    public LocalDateTime getPassTimestamp()                      { return passTimestamp; }
    public void setPassTimestamp(LocalDateTime passTimestamp)    { this.passTimestamp = passTimestamp; }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public String getStatusMessage()                    { return statusMessage; }
    public void setStatusMessage(String statusMessage)  { this.statusMessage = statusMessage; }

    public boolean isFastagEnabled()                    { return fastagEnabled; }
    public void setFastagEnabled(boolean fastagEnabled) { this.fastagEnabled = fastagEnabled; }

    @Override
    public String toString() {
        return String.format(
                "VehiclePassEntity{passId=%d, regNum='%s', type=%s, toll=%.2f, " +
                "balance=%.2f, lane=%d, time=%s, status=%s}",
                passId, registrationNum, vehicleType, tollAmount,
                remainingBalance, laneNumber, passTimestamp, status);
    }
}
package com.tollsystem.exception;

/**
 * Checked exception thrown when a vehicle is not authorized to use a
 * particular toll lane or when FASTag is not activated.
 */
public class UnauthorizedVehicleException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String registrationNum;
    private final String reasonCode;

    /**
     * Constructs the exception with vehicle identification and a reason.
     *
     * @param registrationNum vehicle registration plate
     * @param reasonCode      short reason code (e.g., "NO_FASTAG", "BLACKLISTED")
     */
    public UnauthorizedVehicleException(String registrationNum, String reasonCode) {
        super(String.format(
                "Unauthorized vehicle detected [%s]. Reason: %s. " +
                "Please proceed to the manual toll booth.",
                registrationNum, reasonCode));
        this.registrationNum = registrationNum;
        this.reasonCode      = reasonCode;
    }

    /**
     * Constructs the exception with a fully custom message plus the
     * unauthorized vehicle registration number.
     *
     * @param registrationNum vehicle registration plate
     * @param reasonCode      short reason code
     * @param message         full human-readable message
     */
    public UnauthorizedVehicleException(String registrationNum,
                                        String reasonCode, String message) {
        super(message);
        this.registrationNum = registrationNum;
        this.reasonCode      = reasonCode;
    }

    public String getRegistrationNum() { return registrationNum; }
    public String getReasonCode()      { return reasonCode; }
}
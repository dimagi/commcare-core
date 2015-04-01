/**
 *
 */
package org.javarosa.xml.util;

/**
 * @author ctsims
 */
public class UnfullfilledRequirementsException extends Exception {

    /**
     * This requirement may be ignorable, but the user should be prompted *
     */
    public static int SEVERITY_PROMPT = 1;
    /**
     * Something is missing from the environment, but it should be able to be provided *
     */
    public static int SEVERITY_ENVIRONMENT = 2;
    /**
     * It isn't clear how to correct the problem, and probably is a programmer error *
     */
    public static int SEVERITY_UNKOWN = 4;

    /**
     * The profile is incompatible with the major version of the current CommCare installation *
     */
    public static int REQUIREMENT_MAJOR_APP_VERSION = 1;

    /**
     * The profile is incompatible with the minor version of the current CommCare installation *
     */
    public static int REQUIREMENT_MINOR_APP_VERSION = 2;

    private int severity;
    private int requirement;

    /**
     * Version Numbers if version is incompatible *
     */
    private int maR, miR, maA, miA;

    public UnfullfilledRequirementsException(String message, int severity) {
        this(message, severity, -1, -1, -1, -1, -1);
    }

    public UnfullfilledRequirementsException(String message, int severity, int requirement) {
        this(message, severity, requirement, -1, -1, -1, -1);
    }

    /**
     * Constructor for unfulfilled version requirements.
     *
     * @param message
     * @param severity
     * @param requirement
     * @param requiredMajor
     * @param requiredMinor
     * @param availableMajor
     * @param availableMinor
     */
    public UnfullfilledRequirementsException(String message, int severity,
                                             int requirement,
                                             int requiredMajor, int requiredMinor, int availableMajor, int availableMinor) {
        super(message);
        this.severity = severity;
        this.requirement = requirement;

        this.maR = requiredMajor;
        this.miR = requiredMinor;

        this.maA = availableMajor;
        this.miA = availableMinor;
    }

    /**
     * @return A human readable version string describing the required version
     */
    public String getRequiredVersionString() {
        return maR + "." + miR;
    }

    /**
     * @return A human readable version string describing the available version
     */
    public String getAvailableVesionString() {
        return maA + "." + miA;
    }

    public int getSeverity() {
        return severity;
    }

    public int getRequirementCode() {
        return requirement;
    }
}

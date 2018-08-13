package org.javarosa.xml.util;

/**
 * @author ctsims
 */
public class UnfullfilledRequirementsException extends Exception {

    private final RequirementType requirementType;

    public enum RequirementType {
        /**
         * Default case, nothing special about this
         */
        NONE,

        /**
         * local system can't provide an expected location to store data
         */
        WRITEABLE_REFERENCE,

        /**
         * The profile is incompatible with the major version of the current CommCare installation *
         */
        MAJOR_APP_VERSION,

        /**
         * The profile is incompatible with the minor version of the current CommCare installation *
         */
        MINOR_APP_VERSION,

        /**
         * Indicates that this exception was thrown due to an attempt to install an app that was
         * already installed
         */
        DUPLICATE_APP,

        /**
         * app is targetting another flavour of Commcare than the one running currently
         */
        INCORRECT_TARGET_PACKAGE,

        /**
         * Trying to reinstall a CommCare App using a ccz that belongs to a different CommCare App
         */
        INCORRECT_APP_CCZ
    }

    /**
     * Version Numbers if version is incompatible *
     */
    private final int maR;
    private final int miR;
    private final int maA;
    private final int miA;

    public UnfullfilledRequirementsException(String message) {
        this(message, RequirementType.NONE);
    }

    public UnfullfilledRequirementsException(String message, RequirementType requirementType) {
        this(message, -1, -1, -1, -1, requirementType);
    }

    /**
     * Constructor for unfulfilled version requirements.
     */
    public UnfullfilledRequirementsException(String message,
                                             int requiredMajor, int requiredMinor, int availableMajor, int availableMinor,
                                             RequirementType requirementType) {
        super(message);
        this.maR = requiredMajor;
        this.miR = requiredMinor;
        this.maA = availableMajor;
        this.miA = availableMinor;
        this.requirementType = requirementType;
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

    public RequirementType getRequirementType() {
        return requirementType;
    }

    /**
     * @return true if this exception was thrown due to an attempt at installing a duplicate app
     */
    public boolean isDuplicateException() {
        return requirementType == RequirementType.DUPLICATE_APP;
    }

    /**
     * @return true if this exception was thrown due to an attempt at installing an app targetting a different Commcare package id
     */
    public boolean isIncorrectTargetException() {
        return requirementType == RequirementType.INCORRECT_TARGET_PACKAGE;
    }

    /**
     * @return true if this exception was thrown due to an attempt at recovering a CommCare App using ccz belonging to a different CommCare App
     */
    public boolean isIncorrectCCZException() {
        return requirementType == RequirementType.INCORRECT_APP_CCZ;
    }
}

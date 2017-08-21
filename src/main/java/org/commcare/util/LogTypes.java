package org.commcare.util;

/**
 * Defines different possible LogTypes
 */
public class LogTypes {

    //Log Types:
    /**
     * Fatal problem with one of CommCare's cryptography libraries
     */
    public static final String TYPE_ERROR_CRYPTO = "error-crypto";

    /**
     * Some invariant application assumption has been violated
     */
    public static final String TYPE_ERROR_ASSERTION = "error-state";

    /**
     * Some invariant application assumption has been violated
     */
    public static final String TYPE_ERROR_WORKFLOW = "error-workflow";

    /**
     * There is a problem with the underlying storage layer which is preventing the app from working correctly
     */
    public static final String TYPE_ERROR_STORAGE = "error-storage";

    /**
     * One of the config files (suite, profile, xform, locale, etc) contains something
     * which is invalid and prevented the app from working properly
     */
    public static final String TYPE_ERROR_CONFIG_STRUCTURE = "error-config";

    /**
     * Something bad happened which the app should not have allowed to happen. This
     * category of error should be aggressively caught and addressed by the software team *
     */
    public static final String TYPE_ERROR_DESIGN = "error-design";

    /**
     * Something bad happened because of network connectivity *
     */
    public static final String TYPE_WARNING_NETWORK = "warning-network";

    /**
     * We were incapable of processing or understanding something that the server sent down
     */
    public static final String TYPE_ERROR_SERVER_COMMS = "error-server-comms";

    /**
     * Logs relating to user events (login/logout/restore, etc) *
     */
    public static final String TYPE_USER = "user";

    /**
     * Logs relating to the external files and resources which make up an app *
     */
    public static final String TYPE_RESOURCES = "resources";

    /**
     * Maintenance events (autopurging, cleanups, etc) *
     */
    public static final String TYPE_MAINTENANCE = "maintenance";

    /**
     * Form Entry workflow messages *
     */
    public static final String TYPE_FORM_ENTRY = "form-entry";

    /**
     * Form submission messages *
     */
    public static final String TYPE_FORM_SUBMISSION = "form-submission";

    /**
     * Used to track when we knowingly delete a form record
     */
    public static final String TYPE_FORM_DELETION = "form-deletion";

    /**
     * Problem reported via report activity at home screen *
     */
    public static final String USER_REPORTED_PROBLEM = "user-report";

    /**
     * Used for internal checking of whether or not certain sections of code ever get called
     */
    public static final String SOFT_ASSERT = "soft-assert";

    /**
     * Used for tracking the behavior of the form dump activity
     */
    public static final String TYPE_FORM_DUMP = "form-dump";

    public static final String TYPE_FORCECLOSE = "forceclose";
    public static final String TYPE_GRAPHING = "graphing";
}

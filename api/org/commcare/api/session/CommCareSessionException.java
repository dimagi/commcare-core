package org.commcare.api.session;

/**
 * Wrapper for exceptions or errors that occur during CLI usage
 *
 * Created by ctsims on 8/5/2015.
 */
public class CommCareSessionException extends Exception {
    public CommCareSessionException(String message) {
        super(message);
    }

    //Wrap an exception with no message
    public CommCareSessionException(Exception e) {
        super(e.getMessage());
        this.initCause(e);
    }

    public CommCareSessionException(String message, Exception e) {
        super(message);
        this.initCause(e);
    }
}

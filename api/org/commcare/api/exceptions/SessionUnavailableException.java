/**
 * 
 */
package org.commcare.api.exceptions;

/**
 * @author ctsims
 *
 */
public class SessionUnavailableException extends RuntimeException {

    public SessionUnavailableException() {
        super();
    }

    public SessionUnavailableException(String message) {
        super(message);
    }
}
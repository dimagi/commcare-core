/**
 *
 */
package org.commcare.resources.model;

/**
 * @author ctsims
 */
public class UnresolvedResourceException extends Exception {
    final Resource r;
    final boolean userFacing;

    public UnresolvedResourceException(Resource r, String message) {
        this(r, message, false);
    }

    public UnresolvedResourceException(Resource r, String message, boolean userFacing) {
        super(message);
        this.r = r;
        this.userFacing = userFacing;
    }

    public UnresolvedResourceException(Resource r, Throwable cause, String message, boolean userFacing) {
        super(message, cause);
        this.r = r;
        this.userFacing = userFacing;
    }

    public Resource getResource() {
        return r;
    }

    public boolean isMessageUseful() {
        return userFacing;
    }
}

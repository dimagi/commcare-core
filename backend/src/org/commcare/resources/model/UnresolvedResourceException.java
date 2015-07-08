/**
 *
 */
package org.commcare.resources.model;

/**
 * @author ctsims
 */
public class UnresolvedResourceException extends Exception {
    Resource r;
    boolean userFacing;

    public UnresolvedResourceException(Resource r, String message) {
        this(r, message, false);
    }

    public UnresolvedResourceException(Resource r, String message, boolean userFacing) {
        super(message);
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

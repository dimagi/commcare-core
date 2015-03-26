/**
 *
 */
package org.commcare.resources.model;

/**
 * @author ctsims
 */
public class MissingMediaException extends Exception {
    Resource r;
    boolean userFacing;
    String URI;

    public MissingMediaException(Resource r, String message) {
        this(r, message, message, false);
    }

    public MissingMediaException(Resource r, String message, String uri) {
        this(r, message, uri, false);
    }

    public MissingMediaException(Resource r, String message, boolean userFacing) {
        super(message);
        this.r = r;
        this.userFacing = userFacing;
    }

    public MissingMediaException(Resource r, String message, String uri, boolean userFacing) {
        this(r, message, userFacing);
        URI = uri;
    }

    public Resource getResource() {
        return r;
    }

    public boolean isMessageUseful() {
        return userFacing;
    }

    public String getURI() {
        return URI;
    }

    public boolean equals(Object obj) {

        if (!(obj instanceof MissingMediaException)) {
            return false;
        }

        MissingMediaException mme = (MissingMediaException)obj;

        if (URI == null || (mme.getURI() == null)) {
            return false;
        }
        return ((this.URI).equals(mme.getURI()));
    }

    public String toString() {
        return URI;

    }
}

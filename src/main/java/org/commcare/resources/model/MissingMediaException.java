/**
 *
 */
package org.commcare.resources.model;

/**
 * @author ctsims
 */
public class MissingMediaException extends Exception {
    final Resource r;
    private final MissingMediaExceptionType type;
    private String URI;

    public enum MissingMediaExceptionType {
        FILE_NOT_FOUND,
        FILE_NOT_ACCESSIBLE,
        INVALID_REFERENCE,
        NONE
    }

    public MissingMediaException(Resource r, String message, MissingMediaExceptionType mediaExceptionType) {
        this(r, message, message, mediaExceptionType);
    }

    public MissingMediaException(Resource r, String message, String uri, MissingMediaExceptionType mediaExceptionType) {
        super(message);
        URI = uri;
        this.r = r;
        this.type = mediaExceptionType;
    }

    public Resource getResource() {
        return r;
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

    public MissingMediaExceptionType getType() {
        return type;
    }
}

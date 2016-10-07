package org.commcare.resources.model;

/**
 * Represents an install issue caused by resource having invalid content (like mismatched xml tag)
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class InvalidResourceStructureException extends RuntimeException {
    public final String resourceName;

    public InvalidResourceStructureException(String resourceName, String msg) {
        super(msg);

        this.resourceName = resourceName;
    }
}

package org.commcare.util.exception;

/**
 * Exception raised on making any invalid selections on the entity screens
 */
public class InvalidEntitiesSelectionException extends RuntimeException {

    public InvalidEntitiesSelectionException(String message) {
        super(message);
    }
}

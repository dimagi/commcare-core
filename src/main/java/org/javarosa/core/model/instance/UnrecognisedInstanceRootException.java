package org.javarosa.core.model.instance;

/**
 * Thrown when an instance has an unsupported instance root attached
 */
public class UnrecognisedInstanceRootException extends RuntimeException{
    public UnrecognisedInstanceRootException(String message) {
        super(message);
    }
}

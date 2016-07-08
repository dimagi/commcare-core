package org.javarosa.core.log;

public class FatalException extends WrappedException {

    public FatalException(String message) {
        super(message);
    }

    public FatalException(String message, Exception child) {
        super(message, child);
    }

}

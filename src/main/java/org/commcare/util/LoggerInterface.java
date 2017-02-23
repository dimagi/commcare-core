package org.commcare.util;

/**
 * Created by willpride on 1/3/17.
 */

public interface LoggerInterface  {
    <T extends Exception & XPathLoggableException> void logError(String message, T cause);
    void logError(String message);
}

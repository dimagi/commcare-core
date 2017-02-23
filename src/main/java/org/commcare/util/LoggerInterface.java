package org.commcare.util;

/**
 * Created by willpride on 1/3/17.
 */

public interface LoggerInterface  {
    void logError(String message, Exception cause);
    void logError(String message);
}

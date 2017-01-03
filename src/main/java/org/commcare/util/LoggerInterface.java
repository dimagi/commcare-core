package org.commcare.util;

import org.javarosa.xpath.XPathException;

/**
 * Created by willpride on 1/3/17.
 */

public interface LoggerInterface  {
    void logError(String message, XPathException cause);
    void logError(String message);
}

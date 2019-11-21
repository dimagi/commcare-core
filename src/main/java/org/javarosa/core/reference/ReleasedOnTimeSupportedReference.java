package org.javarosa.core.reference;

import java.text.ParseException;

// Interface for getting released on time for an app from a Reference
public interface ReleasedOnTimeSupportedReference {

    /**
     *
     * @return released on time for apps in milliseconds, -1 if it's not defined
     * @throws ParseException in case date format is not ISO8601
     *
     */
    long getReleasedOnTime() throws ParseException;
}

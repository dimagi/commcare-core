package org.javarosa.xml.util;

import org.kxml2.io.KXmlParser;

/**
 * Invalid Structure Exceptions are thrown when an invalid
 * definition is found while parsing XML defining CommCare
 * Models.
 *
 * @author ctsims
 */
public class InvalidStructureException extends Exception {

    /**
     * @param message A Message associated with the error.
     * @param parser  The parser in the position at which the error was detected.
     */
    public InvalidStructureException(String message, KXmlParser parser) {
        super("Invalid XML Structure(" + parser.getPositionDescription() + "): " + message);
    }

    /**
     * @param message A Message associated with the error.
     * @param parser  The parser in the position at which the error was detected.
     * @param file    The file being parsed
     */
    public InvalidStructureException(String message, String file, KXmlParser parser) {
        super("Invalid XML Structure in document " + file + "(" + parser.getPositionDescription() + "): " + message);
    }
}

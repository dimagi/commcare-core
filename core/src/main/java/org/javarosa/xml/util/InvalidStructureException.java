package org.javarosa.xml.util;

import org.javarosa.core.services.locale.Localization;
import org.kxml2.io.KXmlParser;

/**
 * Invalid Structure Exceptions are thrown when an invalid
 * definition is found while parsing XML defining CommCare
 * Models.
 *
 * @author ctsims
 */
public class InvalidStructureException extends Exception {
    private String localizationKey;
    private String[] localizationParameters;

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

    public InvalidStructureException(String message) {
        super(message);
    }

    private InvalidStructureException(String localizationKey, String[] localizationParameters, String message) {
        super(message);
        this.localizationKey = localizationKey;
        this.localizationParameters = localizationParameters;
    }

    public String getLocalizedMessage() {
        if (localizationKey != null) {
            return Localization.get(localizationKey, localizationParameters);
        } else {
            return getMessage();
        }
    }

    public static InvalidStructureException localizableInvalidStructureException(String localizationKey,
                                                                                 String[] localizationParameters,
                                                                                 String defaultMessage,
                                                                                 KXmlParser parser) {
        return new InvalidStructureException(localizationKey, localizationParameters, defaultMessage);
    }

    public static InvalidStructureException readableInvalidStructureException(String message, KXmlParser parser) {
        String humanReadableMessage =
                message + InvalidStorageStructureException.buildParserMessage(parser);
        return new InvalidStructureException(humanReadableMessage);
    }
}

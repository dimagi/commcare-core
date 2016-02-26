package org.javarosa.xml.util;

import org.kxml2.io.KXmlParser;

/**
 * Invalid storage-based definition encountered while parsing XML.
 * For instance trying to load a non-existent case id.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class InvalidStorageStructureException extends RuntimeException {
    public InvalidStorageStructureException(String message, KXmlParser parser) {
        super(message + buildParserMessage(parser));
    }

    public static String buildParserMessage(KXmlParser parser) {
        String prefix = parser.getPrefix();
        if (prefix != null) {
            return ". Source: <" + prefix + ":" + parser.getName() + "> tag in namespace: " + parser.getNamespace();
        } else {
            return ". Source: <" + parser.getName() + ">";
        }
    }
}

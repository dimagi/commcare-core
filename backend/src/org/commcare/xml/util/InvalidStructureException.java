/**
 * 
 */
package org.commcare.xml.util;

import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 *
 */
public class InvalidStructureException extends Exception {
	public InvalidStructureException(String message, KXmlParser parser) {
		super("Invalid XML Structure(" + parser.getPositionDescription() + "): " + message);
	}
	public InvalidStructureException(String message, String file, KXmlParser parser) {
		super("Invalid XML Structure in document " + file + "(" + parser.getPositionDescription() + "): " + message);
	}
}

/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;

import org.commcare.suite.model.SessionDatum;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class SessionDatumParser extends ElementParser<SessionDatum> {

	public SessionDatumParser(KXmlParser parser) {
		super(parser);
	}

	public SessionDatum parse() throws InvalidStructureException, IOException, XmlPullParserException {
		this.checkNode("datum");
		
		String id = parser.getAttributeValue(null, "id");
		String nodeset = parser.getAttributeValue(null, "nodeset");
		String shortDetail = parser.getAttributeValue(null, "select");
		String longDetail = parser.getAttributeValue(null, "confirm");
		String value = parser.getAttributeValue(null, "value");
		
		SessionDatum datum = new SessionDatum(id, nodeset, shortDetail, longDetail, value);
		return datum;
	}

}

package org.commcare.xml;

import org.commcare.suite.model.Callout;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Parser used in DetailParser to parse the defintions of callouts used in
 * case select and detail views.
 *
 * @author wspride
 */

public class CalloutParser extends ElementParser<Callout> {

    public CalloutParser(KXmlParser parser) {
        super(parser);
    }

    public Callout parse() throws InvalidStructureException, IOException, XmlPullParserException {

        String actionName = parser.getAttributeValue(null, "action");
        String image = parser.getAttributeValue(null, "image");
        String displayName = parser.getAttributeValue(null, "name");

        Callout callout = new Callout(actionName, image, displayName);

        while (nextTagInBlock("lookup")) {
            String tagName = parser.getName();
            if (("extra").equals(tagName)) {
                callout.addExtra(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
            } else if ("response".equals(tagName)) {
                callout.addResponse(parser.getAttributeValue(null, "key"));
            }

        }

        return callout;
    }
}

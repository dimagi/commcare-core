package org.commcare.xml;

import org.commcare.suite.model.Callout;
import org.commcare.suite.model.DetailField;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

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

        Hashtable<String, String> extras = new Hashtable<String, String>();
        Vector<String> responses = new Vector<String>();
        DetailField responseDetailField = null;

        while (nextTagInBlock("lookup")) {
            String tagName = parser.getName();
            if ("extra".equals(tagName)) {
                extras.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
            } else if ("response".equals(tagName)) {
                responses.addElement(parser.getAttributeValue(null, "key"));
            } else if ("field".equals(tagName)) {
                responseDetailField = new DetailFieldParser(parser, null, "'lookup callout detail field'").parse();
            }
        }
        return new Callout(actionName, image, displayName, extras, responses, responseDetailField);
    }
}

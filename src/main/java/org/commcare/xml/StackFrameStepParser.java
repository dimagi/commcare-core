package org.commcare.xml;

import org.commcare.suite.model.StackFrameStep;
import org.commcare.session.SessionFrame;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * @author ctsims
 */
class StackFrameStepParser extends ElementParser<StackFrameStep> {

    StackFrameStepParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public StackFrameStep parse() throws InvalidStructureException, IOException, XmlPullParserException {
        String operation = parser.getName();
        String value = parser.getAttributeValue(null, "value");

        switch (operation) {
            case "datum":
                String datumId = parser.getAttributeValue(null, "id");
                return parseValue(SessionFrame.STATE_UNKNOWN, datumId, value);
            case "rewind":
                return parseValue(SessionFrame.STATE_REWIND, null, value);
            case "mark":
                return parseValue(SessionFrame.STATE_MARK, null, value);
            case "command":
                return parseValue(SessionFrame.STATE_COMMAND_ID, null, value);
            case "query":
                // TODO: id is results instance, value is url, include GET string for now, maybe later use extras for URL args and template="case"
                String queryId = parser.getAttributeValue(null, "storage-instance");
                String queryValue = parser.getAttributeValue(null, "url");
                return parseValue(SessionFrame.STATE_QUERY_REQUEST, queryId, queryValue);
            default:
                throw new InvalidStructureException("<" + operation + "> is not a valid stack frame element!", this.parser);
        }
    }

    private StackFrameStep parseValue(String type, String datumId, String value) throws XmlPullParserException, IOException, InvalidStructureException {
        //TODO: ... require this to have a value!!!! It's not processing this properly
        boolean valueIsXpath;
        if (value == null) {
            //must have a child
            value = parser.nextText();
            //Can we get here, or would this have caused an exception?
            if (value == null) {
                throw new InvalidStructureException("Stack frame element must define a value expression or have a direct value", parser);
            } else {
                value = value.trim();
            }
            valueIsXpath = false;
        } else {
            //parse out the xpath value to double check for errors
            valueIsXpath = true;
        }
        try {
            return new StackFrameStep(type, datumId, value, valueIsXpath);
        } catch (XPathSyntaxException e) {
            throw new InvalidStructureException("Invalid expression for stack frame step definition: " + value + ".\n" + e.getMessage(), parser);
        }
    }
}

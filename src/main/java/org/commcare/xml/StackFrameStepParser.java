package org.commcare.xml;

import org.commcare.session.SessionFrame;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.StackFrameStep;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
        String datumId = parser.getAttributeValue(null, "id");

        switch (operation) {
            case "datum":
                return parseValue(SessionFrame.STATE_UNKNOWN, datumId);
            case "rewind":
                return parseValue(SessionFrame.STATE_REWIND, null);
            case "mark":
                return parseValue(SessionFrame.STATE_MARK, null);
            case "command":
                return parseValue(SessionFrame.STATE_COMMAND_ID, null);
            case "query":
                return parseQuery();
            case "jump":
                return parseJump();
            case "instance-datum":
                return parseValue(SessionFrame.STATE_MULTIPLE_DATUM_VAL, datumId);
            default:
                throw new InvalidStructureException("<" + operation + "> is not a valid stack frame element!", this.parser);
        }
    }

    private StackFrameStep parseQuery() throws InvalidStructureException, IOException, XmlPullParserException {
        String queryId = parser.getAttributeValue(null, "id");
        String url = parser.getAttributeValue(null, "value");
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            String errorMsg = "<query> element has invalid 'value' attribute (" + url + ").";
            throw new InvalidStructureException(errorMsg, parser);
        }

        StackFrameStep step = new StackFrameStep(SessionFrame.STATE_QUERY_REQUEST, queryId, url);
        while (nextTagInBlock("query")) {
            String tagName = parser.getName();
            if ("data".equals(tagName)) {
                QueryData queryData = new QueryDataParser(parser).parse();
                step.addExtra(queryData.getKey(), queryData);
            }
        }
        return step;
    }

    private StackFrameStep parseJump() throws InvalidStructureException, IOException, XmlPullParserException {
        String id = parser.getAttributeValue(null, "id");
        StackFrameStep step = new StackFrameStep(SessionFrame.STATE_SMART_LINK, id, null);
        TextParser textParser = new TextParser(parser);
        nextTag("url");
        nextTag("text");
        step.addExtra("url", textParser.parse());
        return step;
    }

    private StackFrameStep parseValue(String type, String datumId) throws XmlPullParserException, IOException, InvalidStructureException {
        String value = parser.getAttributeValue(null, "value");
        boolean valueIsXpath;
        if (value == null) {
            //must have a child
            value = parser.nextText();
            if (value != null) {
                value = value.trim();
                if (value.isEmpty()) {
                    value = null;
                }
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

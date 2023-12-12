package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.QueryGroup;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class QueryGroupParser extends CommCareElementParser<QueryGroup> {

    public static final String NAME_GROUP = "group";
    private static final String ATTR_KEY = "key";
    private static final String NAME_DISPLAY = "display";

    public QueryGroupParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public QueryGroup parse() throws InvalidStructureException, IOException, XmlPullParserException,
            UnfullfilledRequirementsException {
        checkNode("group");

        String key = parser.getAttributeValue(null, ATTR_KEY);
        DisplayUnit display = null;
    
        while (nextTagInBlock(NAME_GROUP)) {
            if (NAME_DISPLAY.equalsIgnoreCase(parser.getName())) {
                display = parseDisplayBlock();
            } else {
                throw new InvalidStructureException(
                        "Unrecognised node " + parser.getName() + "in validation for group " + key);
            }
        }

        if (key == null) {
            throw new InvalidStructureException("<group> block must define a 'key' attribute", parser);
        }
        if (display == null) {
            throw new InvalidStructureException("<group> block must define a <display> element", parser);
        }

        return new QueryGroup(key, display);
    }
}

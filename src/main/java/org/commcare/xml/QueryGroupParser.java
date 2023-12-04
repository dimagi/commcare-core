package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.QueryGroup;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class QueryGroupParser extends CommCareElementParser<QueryGroup> {

    private static final String NAME_PROMPT = "group";
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
    
        while (nextTagInBlock(NAME_PROMPT)) {
            if (NAME_DISPLAY.equalsIgnoreCase(parser.getName())) {
                display = parseDisplayBlock();
            }
        }

        return new QueryGroup(key, display);
    }
}

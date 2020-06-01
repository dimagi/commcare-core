package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.QueryPrompt;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class QueryPromptParser extends CommCareElementParser<QueryPrompt> {

    private static final String NAME_PROMPT = "prompt";

    public QueryPromptParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public QueryPrompt parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        String appearance = parser.getAttributeValue(null, "appearance");
        String key = parser.getAttributeValue(null, "key");
        String input = parser.getAttributeValue(null, "input");
        DisplayUnit display = null;
        while (nextTagInBlock(NAME_PROMPT)) {
            if ("display".equals(parser.getName().toLowerCase())) {
                display = parseDisplayBlock();
            } else if ("itemset".equals(parser.getName().toLowerCase())){

            }
        }
        return new QueryPrompt(key, appearance, input, display);
    }
}

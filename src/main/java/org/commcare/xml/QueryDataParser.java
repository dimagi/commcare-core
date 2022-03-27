package org.commcare.xml;

import org.commcare.suite.model.ListQueryData;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.ValueQueryData;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for parsing {@code <data>} elements
 */
public class QueryDataParser extends CommCareElementParser<QueryData> {

    public QueryDataParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public QueryData parse() throws InvalidStructureException, XmlPullParserException, IOException {
        checkNode("data");

        String key = parser.getAttributeValue(null, "key");
        String ref = parser.getAttributeValue(null, "ref");
        String nodeset = parser.getAttributeValue(null, "nodeset");
        String exclude = parser.getAttributeValue(null, "exclude");

        if (nextTagInBlock("data")) {
            throw new InvalidStructureException("<data> block does not support nested elements", parser);
        }

        if (ref == null) {
            throw new InvalidStructureException("<data> block must define a 'ref' attribute", parser);
        }

        XPathExpression excludeExpr = null;
        if (exclude != null) {
            excludeExpr = parseXpath(exclude);
        }

        if (nodeset != null) {
            return new ListQueryData(
                    key,
                    XPathReference.getPathExpr(nodeset).getReference(),
                    excludeExpr,
                    XPathReference.getPathExpr(ref)
            );
        }
        return new ValueQueryData(key, parseXpath(ref), excludeExpr);
    }

    private XPathExpression parseXpath(String ref) throws InvalidStructureException {
        try {
            return XPathParseTool.parseXPath(ref);
        } catch (XPathSyntaxException e) {
            String errorMessage = "'ref' value is not a valid xpath expression: " + ref;
            throw new InvalidStructureException(errorMessage, this.parser);
        }
    }
}

package org.commcare.xml;

import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.ValueQueryData;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;

/**
 * Parser for parsing <data> elements
 */
public class QueryDataParser extends CommCareElementParser<QueryData> {

    public QueryDataParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public QueryData parse() throws InvalidStructureException {
        if (!"data".equals(parser.getName())) {
            throw new InvalidStructureException("Expected <data> instead found " + this.parser.getName() + ">", this.parser);
        }

        String key = parser.getAttributeValue(null, "key");
        String ref = parser.getAttributeValue(null, "ref");
        XPathExpression parseXPath;
        try {
            parseXPath = XPathParseTool.parseXPath(ref);
        } catch (XPathSyntaxException e) {
            String errorMessage = "'ref' value is not a valid xpath expression: " + ref;
            throw new InvalidStructureException(errorMessage, this.parser);
        }
        return new ValueQueryData(key, parseXPath);
    }
}

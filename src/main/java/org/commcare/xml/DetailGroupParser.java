package org.commcare.xml;

import org.commcare.suite.model.DetailGroup;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DetailGroupParser extends CommCareElementParser<DetailGroup> {

    public static final String NAME_GROUP = "group";
    public static final String ATTRIBUTE_NAME_FUNCTION = "function";
    public static final String ATTRIBUTE_NAME_HEADER_ROWS = "header-rows";

    public DetailGroupParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public DetailGroup parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode(NAME_GROUP);
        String functionStr = parser.getAttributeValue(null, ATTRIBUTE_NAME_FUNCTION);
        XPathExpression function;
        if (functionStr == null) {
            throw new InvalidStructureException("No function in detail group declaration " + parser.getName(),
                    parser);
        }
        try {
            function = XPathParseTool.parseXPath(functionStr);
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            throw new InvalidStructureException("Invalid XPath function " + functionStr + ". " + e.getMessage(),
                    parser);
        }
        String headerRowsStr = parser.getAttributeValue(null, ATTRIBUTE_NAME_HEADER_ROWS);
        if (headerRowsStr == null) {
            headerRowsStr = "1";
        }
        Integer headerRows;
        try {
            headerRows = Integer.parseInt(headerRowsStr);
        } catch (NumberFormatException e) {
            throw new InvalidStructureException(
                    "non integer value for header-rows " + headerRowsStr + ". " + e.getMessage(), parser);
        }
        return new DetailGroup(function, headerRows);
    }
}

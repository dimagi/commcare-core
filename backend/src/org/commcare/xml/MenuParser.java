package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Menu;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class MenuParser extends CommCareElementParser<Menu> {

    public MenuParser(KXmlParser parser) {
        super(parser);
    }

    /* (non-Javadoc)
     * @see org.javarosa.xml.ElementParser#parse()
     */
    public Menu parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("menu");

        String id = parser.getAttributeValue(null, "id");
        String root = parser.getAttributeValue(null, "root");
        root = root == null ? "root" : root;

        String relevant = parser.getAttributeValue(null, "relevant");
        XPathExpression relevantExpression = null;
        if (relevant != null) {
            try {
                relevantExpression = XPathParseTool.parseXPath(relevant);
            } catch (XPathSyntaxException e) {
                e.printStackTrace();
                throw new InvalidStructureException("Bad module filtering expression {" + relevant + "}", parser);
            }
        }

        getNextTagInBlock("menu");

        DisplayUnit display;
        if (parser.getName().equals("text")) {
            display = new DisplayUnit(new TextParser(parser).parse(), null, null);
        } else if (parser.getName().equals("display")) {
            display = parseDisplayBlock();
            //check that we have a commandText;
            if (display.getText() == null)
                throw new InvalidStructureException("Expected Menu Text in Display block", parser);
        } else {
            throw new InvalidStructureException("Expected either <text> or <display> in menu", parser);
        }


        //name = new TextParser(parser).parse();

        Vector<String> commandIds = new Vector<String>();
        Vector<String> relevantExprs = new Vector<String>();
        while (nextTagInBlock("menu")) {
            checkNode("command");
            commandIds.addElement(parser.getAttributeValue(null, "id"));
            String relevantExpr = parser.getAttributeValue(null, "relevant");
            if (relevantExpr == null) {
                relevantExprs.addElement(null);
            } else {
                try {
                    //Safety checking
                    XPathParseTool.parseXPath(relevantExpr);
                    relevantExprs.addElement(relevantExpr);
                } catch (XPathSyntaxException e) {
                    e.printStackTrace();
                    throw new InvalidStructureException("Bad XPath Expression {" + relevantExpr + "}", parser);
                }
            }
        }

        String[] expressions = new String[relevantExprs.size()];
        relevantExprs.copyInto(expressions);

        Menu m = new Menu(id, root, relevant, relevantExpression, display, commandIds, expressions);
        return m;

    }

}

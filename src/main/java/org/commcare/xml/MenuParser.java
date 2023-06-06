package org.commcare.xml;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Menu;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 */
public class MenuParser extends CommCareElementParser<Menu> {

    public MenuParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public Menu parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("menu");

        String id = parser.getAttributeValue(null, "id");
        String root = parser.getAttributeValue(null, "root");
        root = root == null ? "root" : root;

        Hashtable<String, DataInstance> instances = new Hashtable<>();

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
        AssertionSet assertions = null;

        String style = parser.getAttributeValue(null, "style");

        getNextTagInBlock("menu");

        DisplayUnit display = null;
        if (parser.getName().equals("text")) {
            display = new DisplayUnit(new TextParser(parser).parse());
        } else if (parser.getName().equals("display")) {
            display = parseDisplayBlock();
            //check that we have a commandText;
            if (display.getText() == null)
                throw new InvalidStructureException("Expected Menu Text in Display block", parser);
        } else {
            throw new InvalidStructureException("Expected either <text> or <display> in menu", parser);
        }


        //name = new TextParser(parser).parse();

        Vector<String> commandIds = new Vector<>();
        Vector<String> relevantExprs = new Vector<>();
        while (nextTagInBlock("menu")) {
            String tagName = parser.getName();
            if (tagName.equals("command")) {
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
            } else if (tagName.toLowerCase().equals("instance")) {
                ParseInstance.parseInstance(instances, parser);
            }else if (tagName.equals("assertions")) {
                try {
                    assertions = new AssertionSetParser(parser).parse();
                } catch (InvalidStructureException e) {
                    e.printStackTrace();
                    throw new InvalidStructureException(e.getMessage(), parser);
                }
            }
        }

        String[] expressions = new String[relevantExprs.size()];
        relevantExprs.copyInto(expressions);

        return new Menu(id, root, relevant, relevantExpression, display, commandIds, expressions,
                style, assertions, instances);
    }
}

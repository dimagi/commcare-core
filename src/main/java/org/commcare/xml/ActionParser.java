package org.commcare.xml;

import org.commcare.suite.model.Action;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.StackOperation;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

import static org.commcare.xml.StackOpParser.NAME_STACK;

/**
 * Parses case list actions, which when triggered manipulate the session stack
 *
 * @author ctsims
 */
public class ActionParser extends CommCareElementParser<Action> {

    public static final String NAME_ACTION = "action";

    public ActionParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public Action parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode(NAME_ACTION);

        String iconForActionBarPlacement = parser.getAttributeValue(null, "action-bar-icon");

        DisplayUnit display = null;
        Vector<StackOperation> stackOps = new Vector<>();

        XPathExpression relevantExpr = parseRelevancyExpr();

        String autoLaunch = parser.getAttributeValue(null, "auto_launch");

        String derivedAutoLaunchExpression = deriveAutoLaunchExpression(autoLaunch);
        XPathExpression autoLaunchExpr = null;

        try {
            autoLaunchExpr = XPathParseTool.parseXPath(derivedAutoLaunchExpression);
        } catch (XPathSyntaxException e) {
            String messageBase = "'autoLaunch' doesn't contain a valid xpath expression: ";
            throw new InvalidStructureException(messageBase + autoLaunch, parser);
        }

        boolean redoLast = "true".equals(parser.getAttributeValue(null, "redo_last"));

        while (nextTagInBlock(NAME_ACTION)) {
            if (parser.getName().equals("display")) {
                display = parseDisplayBlock();
            } else if (parser.getName().equals("stack")) {
                StackOpParser sop = new StackOpParser(parser);
                while (this.nextTagInBlock(NAME_STACK)) {
                    stackOps.addElement(sop.parse());
                }
            }
        }

        if (display == null) {
            throw new InvalidStructureException("<action> block must define a <display> element", parser);
        }
        return new Action(display, stackOps, relevantExpr, iconForActionBarPlacement, autoLaunchExpr, redoLast);
    }

    private String deriveAutoLaunchExpression(String autoLaunch) {
        if ("true".equalsIgnoreCase(autoLaunch)) {
            return "true()";
        } else if ("false".equalsIgnoreCase(autoLaunch)) {
            return "false()";
        }
        return autoLaunch;
    }

    private XPathExpression parseRelevancyExpr() throws InvalidStructureException {
        String relevantExprString = parser.getAttributeValue(null, "relevant");
        if (relevantExprString != null) {
            try {
                return XPathParseTool.parseXPath(relevantExprString);
            } catch (XPathSyntaxException e) {
                String messageBase = "'relevant' doesn't contain a valid xpath expression: ";
                throw new InvalidStructureException(messageBase + relevantExprString, parser);
            }
        } else {
            return null;
        }
    }
}

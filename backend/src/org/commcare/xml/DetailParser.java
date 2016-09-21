package org.commcare.xml;

import org.commcare.suite.model.Action;
import org.commcare.suite.model.Callout;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class DetailParser extends CommCareElementParser<Detail> {

    public DetailParser(KXmlParser parser) {
        super(parser);
    }

    public Detail parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("detail");

        String id = parser.getAttributeValue(null, "id");
        String nodeset = parser.getAttributeValue(null, "nodeset");
        String fitAcross = parser.getAttributeValue(null, "fit-across");
        String useUniformUnits = parser.getAttributeValue(null, "uniform-units");
        String forceLandscapeView = parser.getAttributeValue(null, "force-landscape");

        // First fetch the title
        getNextTagInBlock("detail");
        // inside title, should be a text node or a display node as the child
        checkNode("title");
        getNextTagInBlock("title");
        DisplayUnit title;
        if ("text".equals(parser.getName().toLowerCase())) {
            title = new DisplayUnit(new TextParser(parser).parse(), null, null);
        } else {
            title = parseDisplayBlock();
        }

        Callout callout = null;
        Vector<Action> actions = new Vector<>();

        //Now get the headers and templates.
        Vector<Detail> subdetails = new Vector<>();
        Vector<DetailField> fields = new Vector<>();
        OrderedHashtable<String, String> variables = new OrderedHashtable<>();
        String focusFunction = null;

        while (nextTagInBlock("detail")) {
            if ("lookup".equals(parser.getName().toLowerCase())) {
                try {
                    checkNode("lookup");
                    callout = new CalloutParser(parser).parse();
                    parser.nextTag();

                } catch (InvalidStructureException e) {
                    System.out.println("Lookup block not found " + e);
                }
            }
            if ("variables".equals(parser.getName().toLowerCase())) {
                while (nextTagInBlock("variables")) {
                    String function = parser.getAttributeValue(null, "function");
                    if (function == null) {
                        throw new InvalidStructureException("No function in variable declaration for variable " + parser.getName(), parser);
                    }
                    try {
                        XPathParseTool.parseXPath(function);
                    } catch (XPathSyntaxException e) {
                        e.printStackTrace();
                        throw new InvalidStructureException("Invalid XPath function " + function + ". " + e.getMessage(), parser);
                    }
                    variables.put(parser.getName(), function);
                }
                continue;
            }
            if ("focus".equals(parser.getName().toLowerCase())) {
                focusFunction = parser.getAttributeValue(null, "function");
                if (focusFunction == null) {
                    throw new InvalidStructureException("No function in focus declaration " + parser.getName(), parser);
                }
                try {
                    XPathParseTool.parseXPath(focusFunction);
                } catch (XPathSyntaxException e) {
                    e.printStackTrace();
                    throw new InvalidStructureException("Invalid XPath function " + focusFunction + ". " + e.getMessage(), parser);
                }
                continue;
            }
            if (ActionParser.NAME_ACTION.equalsIgnoreCase(parser.getName())) {
                actions.addElement(new ActionParser(parser).parse());
                continue;
            }
            if (parser.getName().equals("detail")) {
                subdetails.addElement(getDetailParser().parse());
            } else {
                DetailField detailField = new DetailFieldParser(parser, getGraphParser(), id).parse();
                fields.addElement(detailField);
            }
        }

        return new Detail(id, title, nodeset, subdetails, fields, variables, actions, callout,
                fitAcross, useUniformUnits, forceLandscapeView, focusFunction);
    }

    protected DetailParser getDetailParser() {
        return new DetailParser(parser);
    }

    protected GraphParser getGraphParser() {
        return new DummyGraphParser(parser);
    }
}

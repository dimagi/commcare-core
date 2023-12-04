package org.commcare.xml;

import org.commcare.suite.model.Action;
import org.commcare.suite.model.Callout;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.DetailGroup;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Global;
import org.commcare.suite.model.Text;
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
    private static final String NAME_NO_ITEMS_TEXT = "no_items_text";

    public DetailParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public Detail parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode("detail");

        String id = parser.getAttributeValue(null, "id");
        String nodeset = parser.getAttributeValue(null, "nodeset");
        String fitAcross = parser.getAttributeValue(null, "fit-across");
        String useUniformUnits = parser.getAttributeValue(null, "uniform-units");
        String forceLandscapeView = parser.getAttributeValue(null, "force-landscape");
        String printTemplatePath = parser.getAttributeValue(null, "print-template");
        String relevancy = parser.getAttributeValue(null, "relevant");
        boolean isLazyLoading = Boolean.parseBoolean(parser.getAttributeValue(null, "lazy_loading"));

        // First fetch the title
        getNextTagInBlock("detail");
        // inside title, should be a text node or a display node as the child
        checkNode("title");
        getNextTagInBlock("title");
        DisplayUnit title;

        if ("text".equals(parser.getName().toLowerCase())) {
            title = new DisplayUnit(new TextParser(parser).parse());
        } else {
            title = parseDisplayBlock();
        }

        Global global = null;
        Callout callout = null;
        Vector<Action> actions = new Vector<>();

        //Now get the headers and templates.
        Text noItemsText = null;
        Vector<Detail> subdetails = new Vector<>();
        Vector<DetailField> fields = new Vector<>();
        OrderedHashtable<String, String> variables = new OrderedHashtable<>();
        String focusFunction = null;
        DetailGroup detailGroup = null;

        while (nextTagInBlock("detail")) {
            if (GlobalParser.NAME_GLOBAL.equals(parser.getName().toLowerCase())) {
                checkNode(GlobalParser.NAME_GLOBAL);
                global = new GlobalParser(parser).parse();
                parser.nextTag();
            }
            if ("lookup".equals(parser.getName().toLowerCase())) {
                checkNode("lookup");
                callout = new CalloutParser(parser).parse();
                parser.nextTag();
            }
            if (NAME_NO_ITEMS_TEXT.equals(parser.getName().toLowerCase())) {
                checkNode("no_items_text");
                getNextTagInBlock("no_items_text");
                if ("text".equals(parser.getName().toLowerCase())) {
                    noItemsText = new TextParser(parser).parse();
                }
                continue;
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
            if (DetailGroupParser.NAME_GROUP.equalsIgnoreCase(parser.getName())) {
                detailGroup = new DetailGroupParser(parser).parse();
                continue;
            }
            if (parser.getName().equals("detail")) {
                subdetails.addElement(getDetailParser().parse());
            } else {
                DetailField detailField = new DetailFieldParser(parser, getGraphParser(), id).parse();
                fields.addElement(detailField);
            }
        }

        return new Detail(id, title, noItemsText, nodeset, subdetails, fields, variables, actions, callout,
                fitAcross, useUniformUnits, forceLandscapeView, focusFunction, printTemplatePath,
                relevancy, global, detailGroup, isLazyLoading);
    }

    protected DetailParser getDetailParser() {
        return new DetailParser(parser);
    }

    protected GraphParser getGraphParser() {
        return new GraphParser(parser);
    }
}

package org.commcare.xml;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.RemoteRequestEntry;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.ValueQueryData;
import org.commcare.suite.model.ViewEntry;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import static org.commcare.xml.StackOpParser.NAME_STACK;

/**
 * @author ctsims
 */
public class EntryParser extends CommCareElementParser<Entry> {
    private static final String FORM_ENTRY_TAG = "entry";
    private static final String VIEW_ENTRY_TAG = "view";
    protected static final String REMOTE_REQUEST_TAG = "remote-request";
    private final String parserBlockTag;

    private EntryParser(KXmlParser parser, String parserBlockTag) {
        super(parser);

        this.parserBlockTag = parserBlockTag;
    }

    public static EntryParser buildViewParser(KXmlParser parser) {
        return new EntryParser(parser, VIEW_ENTRY_TAG);
    }

    public static EntryParser buildEntryParser(KXmlParser parser) {
        return new EntryParser(parser, FORM_ENTRY_TAG);
    }

    public static EntryParser buildRemoteSyncParser(KXmlParser parser) {
        return new EntryParser(parser, REMOTE_REQUEST_TAG);
    }

    @Override
    public Entry parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        this.checkNode(parserBlockTag);

        String xFormNamespace = null;
        Vector<SessionDatum> data = new Vector<>();
        Hashtable<String, DataInstance> instances = new Hashtable<>();

        String commandId = "";
        DisplayUnit display = null;
        Vector<StackOperation> stackOps = new Vector<>();
        AssertionSet assertions = null;
        PostRequest post = null;

        while (nextTagInBlock(parserBlockTag)) {
            String tagName = parser.getName();
            if ("form".equals(tagName)) {
                if (parserBlockTag.equals(VIEW_ENTRY_TAG)) {
                    throw new InvalidStructureException("<" + parserBlockTag + ">'s cannot specify XForms!!", parser);
                }
                xFormNamespace = parser.nextText();
            } else if ("command".equals(tagName)) {
                commandId = parser.getAttributeValue(null, "id");
                display = parseCommandDisplay();
            } else if ("instance".equals(tagName.toLowerCase())) {
                ParseInstance.parseInstance(instances, parser);
            } else if ("session".equals(tagName)) {
                parseSessionData(data);
            } else if ("entity".equals(tagName) || "details".equals(tagName)) {
                throw new InvalidStructureException("Incompatible CaseXML 1.0 elements detected in <" + parserBlockTag + ">. " +
                        tagName + " is not a valid construct in 2.0 CaseXML", parser);
            } else if ("stack".equals(tagName)) {
                parseStack(stackOps);
            } else if ("assertions".equals(tagName)) {
                assertions = new AssertionSetParser(parser).parse();
            } else if ("post".equals(tagName)) {
                post = parsePost();
            }
        }

        if (display == null) {
            throw new InvalidStructureException("<entry> block must define display text details", parser);
        }

        //The server side wasn't generating <view> blocks correctly for a long time, so if we have
        //an entry with no xmlns and no operations, we'll consider that a view.
        boolean isViewEntry = VIEW_ENTRY_TAG.equals(parserBlockTag) ||
                (FORM_ENTRY_TAG.equals(parserBlockTag) &&
                        xFormNamespace == null &&
                        stackOps.size() == 0);

        if (isViewEntry) {
            return new ViewEntry(commandId, display, data, instances, stackOps, assertions);
        } else if (FORM_ENTRY_TAG.equals(parserBlockTag)) {
            return new FormEntry(commandId, display, data, xFormNamespace, instances, stackOps, assertions, post);
        } else if (REMOTE_REQUEST_TAG.equals(parserBlockTag)) {
            if (post == null) {
                throw new RuntimeException(REMOTE_REQUEST_TAG + " must contain a <post> element");
            } else {
                return new RemoteRequestEntry(commandId, display, data, instances, stackOps, assertions, post);
            }
        }

        throw new RuntimeException("Misconfigured entry parser with unsupported '" + parserBlockTag + "' tag.");
    }

    private DisplayUnit parseCommandDisplay() throws InvalidStructureException, IOException, XmlPullParserException {
        parser.nextTag();
        DisplayUnit display = null;
        String tagName = parser.getName();
        if ("text".equals(tagName)) {
            display = new DisplayUnit(new TextParser(parser).parse());
        } else if ("display".equals(tagName)) {
            display = parseDisplayBlock();
            //check that we have text to display;
            if (display.getText() == null) {
                throw new InvalidStructureException("Expected CommandText in Display block", parser);
            }
        }
        return display;
    }

    private void parseSessionData(Vector<SessionDatum> data) throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        while (nextTagInBlock("session")) {
            SessionDatumParser datumParser = new SessionDatumParser(this.parser);
            data.addElement(datumParser.parse());
        }
    }

    private void parseStack(Vector<StackOperation> stackOps) throws InvalidStructureException, IOException, XmlPullParserException {
        StackOpParser sop = new StackOpParser(parser);
        while (this.nextTagInBlock(NAME_STACK)) {
            stackOps.addElement(sop.parse());
        }
    }

    private PostRequest parsePost() throws InvalidStructureException, IOException, XmlPullParserException {
        String urlString = parser.getAttributeValue(null, "url");
        if (urlString == null) {
            throw new InvalidStructureException("Expected 'url' attribute in a <post> structure.",
                    parser);
        }
        URL url;
        try {
            url = new URL(urlString); } catch (MalformedURLException e) {
            throw new InvalidStructureException(
                    "The <post> block's 'url' attribute (" + urlString + ") isn't a valid url.",
                    parser);
        }

        XPathExpression relevantExpr = null;
        String relevantExprString = parser.getAttributeValue(null, "relevant");
        if (relevantExprString != null) {
            try {
                relevantExpr = XPathParseTool.parseXPath(relevantExprString);
            } catch (XPathSyntaxException e) {
                String messageBase = "'relevant' doesn't contain a valid xpath expression: ";
                throw new InvalidStructureException(messageBase + relevantExprString, parser);
            }
        }

        List<QueryData> postData = new ArrayList<QueryData>();
        while (nextTagInBlock("post")) {
            postData.add(new QueryDataParser(parser).parse());
        }
        return new PostRequest(url, relevantExpr, postData);
    }
}

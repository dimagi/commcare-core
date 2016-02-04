package org.commcare.xml;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.RemoteQuery;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.SyncEntry;
import org.commcare.suite.model.ViewEntry;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 */
public class EntryParser extends CommCareElementParser<Entry> {
    private static final String FORM_ENTRY_TAG = "entry";
    private static final String VIEW_ENTRY_TAG = "view";
    private static final String SYNC_REQUEST_TAG = "sync-request";
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
        return new EntryParser(parser, SYNC_REQUEST_TAG);
    }

    @Override
    public Entry parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode(parserBlockTag);

        String xFormNamespace = null;
        Vector<SessionDatum> data = new Vector<SessionDatum>();
        Hashtable<String, DataInstance> instances = new Hashtable<String, DataInstance>();

        String commandId = "";
        DisplayUnit display = null;
        Vector<StackOperation> stackOps = new Vector<StackOperation>();
        AssertionSet assertions = null;
        SyncEntry.SyncPost post = null;
        Vector<RemoteQuery> queries = new Vector<RemoteQuery>();

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
                parseInstance(instances);
            } else if ("session".equals(tagName)) {
                parseSessionData(data, queries);
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

        if (FORM_ENTRY_TAG.equals(parserBlockTag)) {
            return new FormEntry(commandId, display, data, xFormNamespace, instances, stackOps, assertions);
        } else if (VIEW_ENTRY_TAG.equals(parserBlockTag)) {
            return new ViewEntry(commandId, display, data, instances, stackOps, assertions);
        } else if (SYNC_REQUEST_TAG.equals(parserBlockTag)) {
            return new SyncEntry(commandId, display, data, instances, stackOps, assertions, post, queries);
        }

        throw new RuntimeException("Misconfigured entry parser");
    }

    private DisplayUnit parseCommandDisplay() throws InvalidStructureException, IOException, XmlPullParserException {
        parser.nextTag();
        DisplayUnit display = null;
        String tagName = parser.getName();
        if ("text".equals(tagName)) {
            display = new DisplayUnit(new TextParser(parser).parse(), null, null);
        } else if ("display".equals(tagName)) {
            display = parseDisplayBlock();
            //check that we have text to display;
            if (display.getText() == null) {
                throw new InvalidStructureException("Expected CommandText in Display block", parser);
            }
        }
        return display;
    }

    private void parseSessionData(Vector<SessionDatum> data, Vector<RemoteQuery> queries) throws InvalidStructureException, IOException, XmlPullParserException {
        while (nextTagInBlock("session")) {
            if ("query".equals(parser.getName())) {
                queries.addElement(new SessionQueryParser(parser).parse());
            } else {
                SessionDatumParser parser = new SessionDatumParser(this.parser);
                data.addElement(parser.parse());
            }
        }
    }

    private void parseStack(Vector<StackOperation> stackOps) throws InvalidStructureException, IOException, XmlPullParserException {
        StackOpParser sop = new StackOpParser(parser);
        while (this.nextTagInBlock("stack")) {
            stackOps.addElement(sop.parse());
        }
    }

    private void parseInstance(Hashtable<String, DataInstance> instances) {
        String instanceId = parser.getAttributeValue(null, "id");
        String location = parser.getAttributeValue(null, "src");
        instances.put(instanceId, new ExternalDataInstance(location, instanceId));
    }

    private SyncEntry.SyncPost parsePost() {
        // TODO PLM
        return null;
    }
}

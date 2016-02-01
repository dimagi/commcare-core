package org.commcare.xml;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackOperation;
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
    private final boolean isEntry;

    private EntryParser(KXmlParser parser, boolean isEntry) {
        super(parser);

        this.isEntry = isEntry;
    }

    public static EntryParser buildViewParser(KXmlParser parser) {
        return new EntryParser(parser, false);
    }

    public static EntryParser buildEntryParser(KXmlParser parser) {
        return new EntryParser(parser, true);
    }

    @Override
    public Entry parse() throws InvalidStructureException, IOException, XmlPullParserException {
        String block = isEntry ? "entry" : "view";
        this.checkNode(block);

        String xFormNamespace = null;
        Vector<SessionDatum> data = new Vector<SessionDatum>();
        Hashtable<String, DataInstance> instances = new Hashtable<String, DataInstance>();

        String commandId = "";
        DisplayUnit display = null;
        Vector<StackOperation> stackOps = new Vector<StackOperation>();
        AssertionSet assertions = null;

        while (nextTagInBlock(block)) {
            String tagName = parser.getName();
            if ("form".equals(tagName)) {
                if (!isEntry) {
                    throw new InvalidStructureException("<" + block + ">'s cannot specify XForms!!", parser);
                }
                xFormNamespace = parser.nextText();
            } else if ("command".equals(tagName)) {
                commandId = parser.getAttributeValue(null, "id");
                display = parseCommandDisplay();
            } else if ("instance".equals(tagName.toLowerCase())) {
                parseInstance(instances);
            } else if ("session".equals(tagName)) {
                parseSessionData(data);
            } else if ("entity".equals(tagName) || "details".equals(tagName)) {
                throw new InvalidStructureException("Incompatible CaseXML 1.0 elements detected in <" + block + ">. " +
                        tagName + " is not a valid construct in 2.0 CaseXML", parser);
            } else if ("stack".equals(tagName)) {
                parseStack(stackOps);
            } else if ("assertions".equals(tagName)) {
                assertions = new AssertionSetParser(parser).parse();
            }
        }

        if (display == null) {
            throw new InvalidStructureException("<entry> block must define display text details", parser);
        }

        if (isEntry) {
            return new FormEntry(commandId, display, data, xFormNamespace, instances, stackOps, assertions);
        } else {
            return new ViewEntry(commandId, display, data, instances, stackOps, assertions);
        }
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

    private void parseSessionData(Vector<SessionDatum> data) throws InvalidStructureException, IOException, XmlPullParserException {
        while (nextTagInBlock("session")) {
            SessionDatumParser parser = new SessionDatumParser(this.parser);
            data.addElement(parser.parse());
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
}

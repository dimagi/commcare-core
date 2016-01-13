package org.commcare.xml;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.EntryBase;
import org.commcare.suite.model.SessionDatum;
import org.commcare.suite.model.StackOperation;
import org.commcare.suite.model.View;
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
public class EntryParser extends CommCareElementParser<EntryBase> {
    private boolean isEntry = true;

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
    public EntryBase parse() throws InvalidStructureException, IOException, XmlPullParserException {
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
            if (parser.getName().equals("form")) {
                if (!isEntry) {
                    throw new InvalidStructureException("<view>'s cannot specify XForms!!", this.parser);
                }
                xFormNamespace = parser.nextText();
            } else if (parser.getName().equals("command")) {
                commandId = parser.getAttributeValue(null, "id");
                parser.nextTag();
                if (parser.getName().equals("text")) {
                    display = new DisplayUnit(new TextParser(parser).parse(), null, null);
                } else if (parser.getName().equals("display")) {
                    display = parseDisplayBlock();
                    //check that we have text to display;
                    if (display.getText() == null) {
                        throw new InvalidStructureException("Expected CommandText in Display block", parser);
                    }
                }
            } else if ("instance".equals(parser.getName().toLowerCase())) {
                String instanceId = parser.getAttributeValue(null, "id");
                String location = parser.getAttributeValue(null, "src");
                instances.put(instanceId, new ExternalDataInstance(location, instanceId));

            } else if (parser.getName().equals("session")) {
                while (nextTagInBlock("session")) {
                    SessionDatumParser parser = new SessionDatumParser(this.parser);
                    data.addElement(parser.parse());
                }
            } else if (parser.getName().equals("entity") || parser.getName().equals("details")) {
                throw new InvalidStructureException("Incompatible CaseXML 1.0 elements detected in <" + block + ">. " +
                        parser.getName() + " is not a valid construct in 2.0 CaseXML", parser);
            } else if (parser.getName().equals("stack")) {
                StackOpParser sop = new StackOpParser(parser);
                while (this.nextTagInBlock("stack")) {
                    stackOps.addElement(sop.parse());
                }
            } else if (parser.getName().equals("assertions")) {
                assertions = new AssertionSetParser(parser).parse();
            }
        }

        if (display == null) {
            throw new InvalidStructureException("<entry> block must define display text details", parser);
        }

        if (isEntry) {
            return new Entry(commandId, display, data, xFormNamespace, instances, stackOps, assertions);
        } else {
            return new View(commandId, display, data, instances, stackOps, assertions);
        }
    }
}

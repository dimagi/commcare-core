/**
 *
 */
package org.commcare.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 */
public class SuiteParser extends ElementParser<Suite> {

    Suite suite;
    ResourceTable table;
    String resourceGuid;

    public SuiteParser(InputStream suiteStream, ResourceTable table, String resourceGuid) throws IOException {
        super(ElementParser.instantiateParser(suiteStream));
        this.table = table;
        this.resourceGuid = resourceGuid;
    }

    public SuiteParser(KXmlParser parser, ResourceTable table, String resourceGuid) {
        super(parser);
        this.table = table;
        this.resourceGuid = resourceGuid;
    }

    public Suite parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {
        checkNode("suite");

        String sVersion = parser.getAttributeValue(null, "version");
        int version = Integer.parseInt(sVersion);
        Hashtable<String, Detail> details = new Hashtable<String, Detail>();
        Hashtable<String, Entry> entries = new Hashtable<String, Entry>();
        Vector<Menu> menus = new Vector<Menu>();

        try {
            //Now that we've covered being inside of a suite,
            //start traversing.
            parser.next();

            int eventType;
            eventType = parser.getEventType();
            do {
                if (eventType == KXmlParser.END_DOCUMENT) {
                } else if (eventType == KXmlParser.START_TAG) {
                    if (parser.getName().toLowerCase().equals("entry")) {
                        Entry e = new EntryParser(parser).parse();
                        entries.put(e.getCommandId(), e);
                    } else if (parser.getName().toLowerCase().equals("view")) {
                        Entry e = new EntryParser(parser, false).parse();
                        entries.put(e.getCommandId(), e);
                    } else if (parser.getName().toLowerCase().equals("locale")) {
                        String localeKey = parser.getAttributeValue(null, "language");
                        //resource def
                        parser.nextTag();
                        Resource r = new ResourceParser(parser, maximumResourceAuthority).parse();
                        table.addResource(r, table.getInstallers().getLocaleFileInstaller(localeKey), resourceGuid);
                    } else if (parser.getName().toLowerCase().equals("media")) {
                        String path = parser.getAttributeValue(null, "path");
                        //Can be an arbitrary number of resources inside of a media block.
                        while (this.nextTagInBlock("media")) {
                            Resource r = new ResourceParser(parser, maximumResourceAuthority).parse();
                            table.addResource(r, table.getInstallers().getMediaInstaller(path), resourceGuid);
                        }
                    } else if (parser.getName().toLowerCase().equals("xform")) {
                        //skip xform stuff for now
                        parser.nextTag();
                        Resource r = new ResourceParser(parser, maximumResourceAuthority).parse();
                        table.addResource(r, table.getInstallers().getXFormInstaller(), resourceGuid);
                    } else if (parser.getName().toLowerCase().equals("detail")) {
                        Detail d = new DetailParser(parser).parse();
                        details.put(d.getId(), d);
                    } else if (parser.getName().toLowerCase().equals("menu")) {
                        Menu m = new MenuParser(parser).parse();
                        menus.addElement(m);
                    } else if (parser.getName().toLowerCase().equals("fixture")) {
                        //this one automatically commits the fixture to the global memory
                        if (!inValidationMode()) {
                            new FixtureXmlParser(parser, false, getFixtureStorage()).parse();
                        }
                    } else {
                        System.out.println("Unrecognized Tag: " + parser.getName());
                    }
                } else if (eventType == KXmlParser.END_TAG) {
                    //we shouldn't ever get this I don't believe, maybe on the last node?
                } else if (eventType == KXmlParser.TEXT) {
                    //Shouldn't ever get this (Delete the if, if so).
                }
                eventType = parser.next();
            } while (eventType != KXmlParser.END_DOCUMENT);

            suite = new Suite(version, details, entries, menus);
            return suite;


        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new InvalidStructureException("Pull Parse Exception, malformed XML.", parser);
        } catch (StorageFullException e) {
            e.printStackTrace();
            //BUT not really! This should maybe be added to the high level declaration
            //instead? Or maybe there should be a more general Resource Management Exception?
            throw new InvalidStructureException("Problem storing parser suite XML", parser);
        }
    }

    int maximumResourceAuthority = -1;

    public void setMaximumAuthority(int authority) {
        maximumResourceAuthority = authority;
    }

    protected IStorageUtilityIndexed<FormInstance> getFixtureStorage() {
        return (IStorageUtilityIndexed<FormInstance>)StorageManager.getStorage("fixture");
    }

    protected boolean inValidationMode() {
        return false;
    }

}

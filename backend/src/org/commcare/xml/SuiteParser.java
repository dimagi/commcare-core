package org.commcare.xml;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.SyncEntry;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Parses a suite file resource and creates the associated object 
 * containing the menu, detail, entry, etc definitions. This parser
 * will also create models for any resource installers that are defined
 * by the suite file and add them to the resource table provided
 * with the suite resource as the parent, that behavior can be skipped
 * by setting a flag if the resources have already been promised.
 * 
 * @author ctsims
 */
public class SuiteParser extends ElementParser<Suite> {
    private final IStorageUtilityIndexed<FormInstance> fixtureStorage;

    private ResourceTable table;
    private String resourceGuid;
    private int maximumResourceAuthority = -1;
    private boolean skipResources = false;

    public SuiteParser(InputStream suiteStream,
                       ResourceTable table,
                       String resourceGuid) throws IOException {
        super(ElementParser.instantiateParser(suiteStream));
        this.table = table;
        this.resourceGuid = resourceGuid;
        this.fixtureStorage = (IStorageUtilityIndexed<FormInstance>)StorageManager.getStorage(FormInstance.STORAGE_KEY);
    }

    public SuiteParser(InputStream suiteStream,
                       ResourceTable table,
                       String resourceGuid,
                       IStorageUtilityIndexed<FormInstance> fixtureStorage) throws IOException {
        super(ElementParser.instantiateParser(suiteStream));

        this.table = table;
        this.resourceGuid = resourceGuid;
        this.fixtureStorage = fixtureStorage;
    }

    public Suite parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
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

            int eventType = parser.getEventType();
            do {
                if (eventType == KXmlParser.START_TAG) {
                    if (parser.getName().toLowerCase().equals("entry")) {
                        Entry e = EntryParser.buildEntryParser(parser).parse();
                        entries.put(e.getCommandId(), e);
                    } else if (parser.getName().toLowerCase().equals("view")) {
                        Entry e = EntryParser.buildViewParser(parser).parse();
                        entries.put(e.getCommandId(), e);
                    } else if (parser.getName().toLowerCase().equals("sync-request")) {
                        Entry syncEntry = EntryParser.buildRemoteSyncParser(parser).parse();
                        entries.put(syncEntry.getCommandId(), syncEntry);
                    } else if (parser.getName().toLowerCase().equals("locale")) {
                        String localeKey = parser.getAttributeValue(null, "language");
                        //resource def
                        parser.nextTag();
                        Resource r = new ResourceParser(parser, maximumResourceAuthority).parse();
                        if(!skipResources) {
                            table.addResource(r, table.getInstallers().getLocaleFileInstaller(localeKey), resourceGuid);
                        }
                    } else if (parser.getName().toLowerCase().equals("media")) {
                        String path = parser.getAttributeValue(null, "path");
                        //Can be an arbitrary number of resources inside of a media block.
                        while (this.nextTagInBlock("media")) {
                            Resource r = new ResourceParser(parser, maximumResourceAuthority).parse();
                            if(!skipResources) {
                                table.addResource(r, table.getInstallers().getMediaInstaller(path), resourceGuid);
                            }
                        }
                    } else if (parser.getName().toLowerCase().equals("xform")) {
                        //skip xform stuff for now
                        parser.nextTag();
                        Resource r = new ResourceParser(parser, maximumResourceAuthority).parse();
                        if(!skipResources) {
                            table.addResource(r, table.getInstallers().getXFormInstaller(), resourceGuid);
                        }
                    } else if (parser.getName().toLowerCase().equals("detail")) {
                        Detail d = getDetailParser().parse();
                        details.put(d.getId(), d);
                    } else if (parser.getName().toLowerCase().equals("menu")) {
                        Menu m = new MenuParser(parser).parse();
                        menus.addElement(m);
                    } else if (parser.getName().toLowerCase().equals("fixture")) {
                        //this one automatically commits the fixture to the global memory
                        if (!inValidationMode()) {
                            new FixtureXmlParser(parser, false, fixtureStorage).parse();
                        }
                    } else {
                        System.out.println("Unrecognized Tag: " + parser.getName());
                    }
                }
                eventType = parser.next();
            } while (eventType != KXmlParser.END_DOCUMENT);

            return new Suite(version, details, entries, menus);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            throw new InvalidStructureException("Pull Parse Exception, malformed XML.", parser);
        } catch (StorageFullException e) {
            e.printStackTrace();
            //BUT not really! This should maybe be added to the high level declaration
            //instead? Or maybe there should be a more general Resource Management Exception?
            throw new InvalidStructureException("Problem storing parser suite XML", parser);
        }
    }

    public void setMaximumAuthority(int authority) {
        maximumResourceAuthority = authority;
    }

    protected boolean inValidationMode() {
        return false;
    }

    /**
     * If set to true, the parser won't process adding incoming resources to the resource table.
     * This is helpful if the suite is being processed during a non-install phase
     */
    public void setSkipResources(boolean skipResources) {
        this.skipResources = skipResources;
    }

    protected DetailParser getDetailParser() {
        return new DetailParser(parser);
    }
}

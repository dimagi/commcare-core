package org.commcare.xml;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
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

    /**
     * If set to true, the parser won't process adding incoming resources to the resource table.
     * This is helpful if the suite is being processed during a non-install phase
     */
    private final boolean skipResources;
    private final boolean isValidationPass;
    private final boolean isUpgrade;

    public SuiteParser(InputStream suiteStream,
                       ResourceTable table,
                       String resourceGuid,
                       IStorageUtilityIndexed<FormInstance> fixtureStorage) throws IOException {
        super(ElementParser.instantiateParser(suiteStream));
        this.table = table;
        this.resourceGuid = resourceGuid;
        this.fixtureStorage = fixtureStorage;
        this.skipResources = false;
        this.isValidationPass = false;
        this.isUpgrade = false;
    }

    public SuiteParser(InputStream suiteStream,
                          ResourceTable table, String resourceGuid,
                          IStorageUtilityIndexed<FormInstance> fixtureStorage,
                          boolean skipResources, boolean isValidationPass,
                          boolean isUpgrade) throws IOException {
        super(ElementParser.instantiateParser(suiteStream));

        this.table = table;
        this.resourceGuid = resourceGuid;
        this.fixtureStorage = fixtureStorage;
        this.skipResources = skipResources;
        this.isValidationPass = isValidationPass;
        this.isUpgrade = isUpgrade;
    }

    @Override
    public Suite parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
        checkNode("suite");

        String sVersion = parser.getAttributeValue(null, "version");
        int version = Integer.parseInt(sVersion);
        Hashtable<String, Detail> details = new Hashtable<>();
        Hashtable<String, Entry> entries = new Hashtable<>();

        Vector<Menu> menus = new Vector<>();

        try {
            //Now that we've covered being inside of a suite,
            //start traversing.
            parser.next();

            int eventType = parser.getEventType();
            do {
                if (eventType == KXmlParser.START_TAG) {
                    String tagName = parser.getName().toLowerCase();
                    switch (tagName) {
                        case "entry":
                            Entry entry = EntryParser.buildEntryParser(parser).parse();
                            entries.put(entry.getCommandId(), entry);
                            break;
                        case "view":
                            Entry viewEntry = EntryParser.buildViewParser(parser).parse();
                            entries.put(viewEntry.getCommandId(), viewEntry);
                            break;
                        case EntryParser.REMOTE_REQUEST_TAG:
                            Entry remoteRequestEntry = EntryParser.buildRemoteSyncParser(parser).parse();
                            entries.put(remoteRequestEntry.getCommandId(), remoteRequestEntry);
                            break;
                        case "locale":
                            String localeKey = parser.getAttributeValue(null, "language");
                            //resource def
                            parser.nextTag();
                            Resource localeResource = new ResourceParser(parser, maximumResourceAuthority).parse();
                            if (!skipResources) {
                                table.addResource(localeResource, table.getInstallers().getLocaleFileInstaller(localeKey), resourceGuid);
                            }
                            break;
                        case "media":
                            String path = parser.getAttributeValue(null, "path");
                            //Can be an arbitrary number of resources inside of a media block.
                            while (this.nextTagInBlock("media")) {
                                Resource mediaResource = new ResourceParser(parser, maximumResourceAuthority).parse();
                                if (!skipResources) {
                                    table.addResource(mediaResource, table.getInstallers().getMediaInstaller(path), resourceGuid);
                                }
                            }
                            break;
                        case "xform":
                            //skip xform stuff for now
                            parser.nextTag();
                            Resource xformResource = new ResourceParser(parser, maximumResourceAuthority).parse();
                            if (!skipResources) {
                                table.addResource(xformResource, table.getInstallers().getXFormInstaller(), resourceGuid);
                            }
                            break;
                        case "user-restore":
                            parser.nextTag();
                            break;
                        case "detail":
                            Detail d = getDetailParser().parse();
                            details.put(d.getId(), d);
                            break;
                        case "menu":
                            Menu m = new MenuParser(parser).parse();
                            menus.addElement(m);
                            break;
                        case "fixture":
                            if (!isValidationPass) {
                                // commit fixture to the memory, overwriting existing
                                // fixture only during first init after app upgrade
                                new FixtureXmlParser(parser, isUpgrade, fixtureStorage).parse();
                            }
                            break;
                        default:
                            System.out.println("Unrecognized Tag: " + parser.getName());
                            break;
                    }
                }
                eventType = parser.next();
            } while (eventType != KXmlParser.END_DOCUMENT);

            return new Suite(version, details, entries, menus);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            throw new InvalidStructureException("Pull Parse Exception, malformed XML.", parser);
        }
    }

    public void setMaximumAuthority(int authority) {
        maximumResourceAuthority = authority;
    }

    protected DetailParser getDetailParser() {
        return new DetailParser(parser);
    }
}

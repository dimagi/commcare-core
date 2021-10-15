package org.commcare.util;

import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.Endpoint;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.OfflineUserRestore;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.Property;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * TODO: This isn't really a great candidate for a
 * singleton interfaces. It should almost certainly be
 * a more broad code-based installer/registration
 * process or something.
 *
 * Also: It shares a lot of similarities with the
 * Context app object in j2me. Maybe we should roll
 * some of that in.
 *
 * @author ctsims
 */
public class CommCarePlatform {
    // TODO: We should make this unique using the parser to invalidate this ID or something
    public static final String APP_PROFILE_RESOURCE_ID = "commcare-application-profile";
    private int profile;
    private Profile cachedProfile;

    private OfflineUserRestore offlineUserRestore;

    private StorageManager storageManager;
    private PropertyManager propertyManager;

    private final int majorVersion;
    private final int minorVersion;
    private final int minimalVersion;
    private final Vector<Suite> installedSuites;

    public CommCarePlatform(int majorVersion, int minorVersion, int minimalVersion, StorageManager storageManager) {
        this(majorVersion, minorVersion, minimalVersion);
        this.storageManager = storageManager;
        storageManager.registerStorage(PropertyManager.STORAGE_KEY, Property.class);
        this.propertyManager = new PropertyManager(storageManager.getStorage(PropertyManager.STORAGE_KEY));
    }

    public CommCarePlatform(int majorVersion, int minorVersion, int minimalVersion) {
        profile = -1;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.minimalVersion = minimalVersion;
        installedSuites = new Vector<>();
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getMinimalVersion() {
        return minimalVersion;
    }

    public Profile getCurrentProfile() {
        if (cachedProfile == null) {
            this.cachedProfile = (Profile)storageManager.getStorage(Profile.STORAGE_KEY).read(profile);
        }
        return cachedProfile;
    }

    public Vector<Suite> getInstalledSuites() {
        if (!installedSuites.isEmpty()) {
            return installedSuites;
        }
        IStorageUtilityIndexed utility = storageManager.getStorage(Suite.STORAGE_KEY);
        IStorageIterator iterator = utility.iterate();
        while(iterator.hasMore()){
            installedSuites.addElement((Suite)utility.read(iterator.nextID()));
        }
        return installedSuites;
    }

    public Detail getDetail(String detailId) {
        for(Suite s : getInstalledSuites()) {
            Detail d = s.getDetail(detailId);
            if(d != null) {
                return d;
            }
        }
        return null;
    }

    public Entry getEntry(String entryId) {
        for(Suite s : getInstalledSuites()) {
            Entry e = s.getEntry(entryId);
            if(e != null) {
                return e;
            }
        }
        return null;
    }

    public Endpoint getEndpoint(String endpointId) {
        for(Suite s : getInstalledSuites()) {
            Endpoint endpoint = s.getEndpoint(endpointId);
            if(endpoint != null) {
                return endpoint;
            }
        }
        return null;
    }

    public Endpoint getEndpointByCommand(String commandId) {
        for(Suite s : getInstalledSuites()) {
            for(Endpoint endpoint : s.getEndpoints().values()) {
                if (commandId.equals(endpoint.getCommandId())) {
                    return endpoint;
                }
            }
        }
        return null;
    }

    public Hashtable<String, Endpoint> getAllEndpoints() {
        Hashtable<String, Endpoint> allEndpoints = new Hashtable<>();
        for(Suite s : getInstalledSuites()) {
            allEndpoints.putAll(s.getEndpoints());
        }
        return allEndpoints;
    }

    public void setProfile(Profile p) {
        this.profile = p.getID();
        this.cachedProfile = p;
    }

    public void registerSuite(Suite s) {
        installedSuites.addElement(s);
    }

    /**
     * Register installed resources in the table with this CommCare instance
     *
     * @param global Table with fully-installed resources
     */
    public void initialize(ResourceTable global, boolean isUpgrade) throws ResourceInitializationException {
        global.initializeResources(this, isUpgrade);
    }

    public void clearAppState() {
        //Clear out any app state
        profile = -1;
    }

    public Hashtable<String, Entry> getCommandToEntryMap() {
        Vector<Suite> installed = getInstalledSuites();
        Hashtable<String, Entry> merged = new Hashtable<>();

        for (Suite s : installed) {
            Hashtable<String, Entry> entriesInSuite = s.getEntries();
            for (Enumeration en = entriesInSuite.keys(); en.hasMoreElements(); ) {
                String commandId = (String)en.nextElement();
                merged.put(commandId, entriesInSuite.get(commandId));
            }
        }
        return merged;
    }

    /**
     * Given an form entry object, return the module's id that contains it.
     *
     * @param formEntry Get the module's id that contains this Entry
     *
     * @return The ID of the module that contains the provided entry. Null if
     * the entry can't be found in the installed suites.
     */
    public String getModuleNameForEntry(FormEntry formEntry) {
        Vector<Suite> installed = getInstalledSuites();

        for (Suite suite : installed) {
            for (Enumeration e = suite.getEntries().elements(); e.hasMoreElements(); ) {
                FormEntry suiteEntry = (FormEntry)e.nextElement();
                if (suiteEntry.getCommandId().equals(formEntry.getCommandId())) {
                    return suite.getMenus().firstElement().getId();
                }
            }
        }
        return null;
    }

    public String getMenuDisplayStyle(String menuId) {
        Vector<Suite> installed = getInstalledSuites();
        String commonDisplayStyle = null;
        for(Suite s : installed) {
            List<Menu> menusWithId = s.getMenusWithId(menuId);
            if (menusWithId != null) {
                for (Menu m : menusWithId) {
                    if (m.getStyle() != null) {
                        if (commonDisplayStyle != null && !m.getStyle().equals(commonDisplayStyle)) {
                            return null;
                        }
                        commonDisplayStyle = m.getStyle();
                    }
                }
            }
        }
        return commonDisplayStyle;
    }

    public OfflineUserRestore getDemoUserRestore() {
        return offlineUserRestore;
    }

    public void registerDemoUserRestore(OfflineUserRestore offlineUserRestore) {
        this.offlineUserRestore = offlineUserRestore;
    }

    public IStorageUtilityIndexed<FormInstance> getFixtureStorage() {
        storageManager.registerStorage("fixture", FormInstance.class);
        return storageManager.getStorage("fixture");
    }

    public PropertyManager getPropertyManager() {
        return propertyManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}

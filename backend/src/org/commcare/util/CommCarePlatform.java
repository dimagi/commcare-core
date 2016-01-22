package org.commcare.util;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceTable;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;

import java.util.Enumeration;
import java.util.Hashtable;
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
public class CommCarePlatform implements CommCareInstance {
    // TODO: We should make this unique using the parser to invalidate this ID or something
    public static final String APP_PROFILE_RESOURCE_ID = "commcare-application-profile";

    private Vector<Integer> suites;
    private int profile;

    private int majorVersion;
    private int minorVersion;

    public CommCarePlatform(int majorVersion, int minorVersion) {
        profile = -1;
        suites = new Vector<Integer>();
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }


    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public Profile getCurrentProfile() {
        return (Profile)(StorageManager.getStorage(Profile.STORAGE_KEY).read(profile));
    }

    public Vector<Suite> getInstalledSuites() {
        Vector<Suite> installedSuites = new Vector<Suite>();
        IStorageUtility utility = StorageManager.getStorage(Suite.STORAGE_KEY);

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

    public void setProfile(Profile p) {
        this.profile = p.getID();
    }


    public void registerSuite(Suite s) {
        this.suites.addElement(new Integer(s.getID()));
    }

    /**
     * Register installed resources in the table with this CommCare instance
     *
     * @param global Table with fully-installed resources
     */
    public void initialize(ResourceTable global) {
        try {
            global.initializeResources(this);
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing Resource! " + e.getMessage());
        }
    }
    public void clearAppState() {
        //Clear out any app state
        profile = -1;
        suites.removeAllElements();
    }

    public Hashtable<String, Entry> getMenuMap() {
        Vector<Suite> installed = getInstalledSuites();
        Hashtable<String, Entry> merged = new Hashtable<String, Entry>();

        for (Suite s : installed) {
            Hashtable<String, Entry> table = s.getEntries();
            for (Enumeration en = table.keys(); en.hasMoreElements(); ) {
                String key = (String)en.nextElement();
                merged.put(key, table.get(key));
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

    public static Vector<Resource> getResourceListFromProfile(ResourceTable master) {
        Vector<Resource> unresolved = new Vector<Resource>();
        Vector<Resource> resolved = new Vector<Resource>();
        Resource r = master.getResourceWithId(APP_PROFILE_RESOURCE_ID);
        if (r == null) {
            return resolved;
        }
        unresolved.addElement(r);
        while (unresolved.size() > 0) {
            Resource current = unresolved.firstElement();
            unresolved.removeElement(current);
            resolved.addElement(current);
            Vector<Resource> children = master.getResourcesForParent(current.getRecordGuid());
            for (Resource child : children) {
                unresolved.addElement(child);
            }
        }
        return resolved;
    }

    public String getMenuDisplayStyle(String menuId) {
        Vector<Suite> installed = getInstalledSuites();
        String commonDisplayStyle = null;
        for(Suite s : installed) {
            for(Menu m : s.getMenus()) {
                if(menuId.equals(m.getId())) {
                    if(m.getStyle() != null) {
                        if(commonDisplayStyle != null && !m.getStyle().equals(commonDisplayStyle)){
                            return null;
                        }
                        commonDisplayStyle = m.getStyle();
                    }
                }
            }
        }
        return commonDisplayStyle;
    }
}

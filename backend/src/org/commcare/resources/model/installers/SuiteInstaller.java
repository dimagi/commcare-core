/**
 *
 */
package org.commcare.resources.model.installers;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareInstance;
import org.commcare.xml.SuiteParser;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.SizeBoundUniqueVector;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author ctsims
 */
public class SuiteInstaller extends CacheInstaller<Suite> {

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
     */
    public boolean initialize(CommCareInstance instance) throws ResourceInitializationException {
        instance.registerSuite(storage().read(cacheLocation));
        return true;
    }

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
     */
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    protected String getCacheKey() {
        return Suite.STORAGE_KEY;
    }

    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException {
        if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
            //If it's in the cache, we should just get it from there
            return false;
        } else {

            InputStream incoming = null;
            try {
                incoming = ref.getStream();
                SuiteParser parser = new SuiteParser(incoming, table, r.getRecordGuid());
                if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
                    parser.setMaximumAuthority(Resource.RESOURCE_AUTHORITY_REMOTE);
                }
                Suite s = parser.parse();
                storage().write(s);
                cacheLocation = s.getID();

                table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);

                //TODOD:
                //Add a resource location for r for its cache location
                //so it can be uninstalled appropriately.
                return true;
            } catch (InvalidStructureException e) {
                throw new UnresolvedResourceException(r, e.getMessage(), true);
            } catch (StorageFullException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                throw new UnreliableSourceException(r, e.getMessage());
            } catch (XmlPullParserException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (incoming != null) {
                        incoming.close();
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problems) {

        SizeBoundUniqueVector sizeBoundProblems = (SizeBoundUniqueVector)problems;

        InstallerUtil.checkMedia(r, Localization.get("icon.demo.path"), sizeBoundProblems, InstallerUtil.MediaType.IMAGE);
        InstallerUtil.checkMedia(r, Localization.get("icon.login.path"), sizeBoundProblems, InstallerUtil.MediaType.IMAGE);

        //Check to see whether the formDef exists and reads correctly
        Suite mSuite;
        try {
            mSuite = storage().read(cacheLocation);
        } catch (Exception e) {
            e.printStackTrace();
            sizeBoundProblems.addElement(new MissingMediaException(r, "Suite did not properly save into persistent storage"));
            return true;
        }
        //Otherwise, we want to figure out if the form has media, and we need to see whether it's properly
        //available
        try {
            Vector<Menu> menus = mSuite.getMenus();
            Iterator e = menus.iterator();

            while (e.hasNext()) {
                Menu mMenu = (Menu)e.next();

                String aURI = mMenu.getAudioURI();
                if (aURI != null) {
                    InstallerUtil.checkMedia(r, aURI, sizeBoundProblems, InstallerUtil.MediaType.AUDIO);
                }

                String iURI = mMenu.getImageURI();
                if (iURI != null) {
                    InstallerUtil.checkMedia(r, iURI, sizeBoundProblems, InstallerUtil.MediaType.IMAGE);
                }
            }
        } catch (Exception exc) {
            System.out.println("fail: " + exc.getMessage());
            System.out.println("fail: " + exc.toString());
        }
        return problems.size() != 0;
    }
}

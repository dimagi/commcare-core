package org.commcare.resources.model.installers;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.commcare.xml.SuiteParser;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.SizeBoundUniqueVector;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * @author ctsims
 */
public class SuiteInstaller extends CacheInstaller<Suite> {

    public SuiteInstaller(){}

    public SuiteInstaller(IStorageIndexedFactory factory) {
        super(factory);
    }

    @Override
    public boolean initialize(CommCarePlatform instance, boolean isUpgrade) {
        instance.registerSuite(storage().read(cacheLocation));
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    @Override
    protected String getCacheKey() {
        return Suite.STORAGE_KEY;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref,
                           ResourceTable table, CommCarePlatform instance,
                           boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException {
        if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
            //If it's in the cache, we should just get it from there
            return false;
        } else {
            InputStream incoming = null;
            try {
                incoming = ref.getStream();
                SuiteParser parser = new SuiteParser(incoming, table, r.getRecordGuid(), instance.getFixtureStorage());
                if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
                    parser.setMaximumAuthority(Resource.RESOURCE_AUTHORITY_REMOTE);
                }
                Suite s = parser.parse();
                storage().write(s);
                cacheLocation = s.getID();

                table.commitCompoundResource(r, Resource.RESOURCE_STATUS_INSTALLED);

                // TODO: Add a resource location for r for its cache location
                // so it can be uninstalled appropriately.
                return true;
            } catch (InvalidStructureException e) {
                throw new UnresolvedResourceException(r, e.getMessage(), true);
            } catch (StorageFullException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                throw new UnreliableSourceException(r, e.getMessage());
            } catch (XmlPullParserException e) {
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

    @Override
    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problems) {
        SizeBoundUniqueVector<MissingMediaException> sizeBoundProblems =
                (SizeBoundUniqueVector<MissingMediaException>)problems;

        InstallerUtil.checkMedia(r, Localization.get("icon.demo.path"), sizeBoundProblems, InstallerUtil.MediaType.IMAGE);
        InstallerUtil.checkMedia(r, Localization.get("icon.login.path"), sizeBoundProblems, InstallerUtil.MediaType.IMAGE);

        //Check to see whether the formDef exists and reads correctly
        Suite suite;
        try {
            suite = storage().read(cacheLocation);
        } catch (Exception e) {
            e.printStackTrace();
            sizeBoundProblems.addElement(new MissingMediaException(r, "Suite did not properly save into persistent storage"));
            return true;
        }
        //Otherwise, we want to figure out if the form has media, and we need to see whether it's properly
        //available
        try {
            for (Menu menu : suite.getMenus()) {
                String aURI = menu.getAudioURI();
                if (aURI != null) {
                    InstallerUtil.checkMedia(r, aURI, sizeBoundProblems, InstallerUtil.MediaType.AUDIO);
                }

                String iURI = menu.getImageURI();
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

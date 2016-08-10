package org.commcare.resources.model.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.OfflineUserRestore;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.io.IOException;

/**
 * Install user restore xml file present in app for use in offline logins.
 * Used for providing a demo user restore.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class UserRestoreInstaller extends CacheInstaller<OfflineUserRestore> {
    @Override
    protected String getCacheKey() {
        return OfflineUserRestore.STORAGE_KEY;
    }

    @Override
    public boolean initialize(CommCareInstance instance, boolean isUpgrade)
            throws ResourceInitializationException {
        instance.registerDemoUserRestore(storage().read(cacheLocation));
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location,
                           Reference ref, ResourceTable table,
                           CommCareInstance instance, boolean upgrade)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {
        try {
            OfflineUserRestore offlineUserRestore = OfflineUserRestore.buildInMemoryUserRestore(ref.getStream());
            storage().write(offlineUserRestore);
            if (upgrade) {
                table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
            } else {
                table.commit(r, Resource.RESOURCE_STATUS_UPGRADE);
            }
            cacheLocation = offlineUserRestore.getID();
        } catch (IOException e) {
            throw new UnresolvedResourceException(r, e.getMessage());
        }
        return true;
    }
}

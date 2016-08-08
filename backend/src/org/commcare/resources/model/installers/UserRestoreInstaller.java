package org.commcare.resources.model.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.UserRestore;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class UserRestoreInstaller extends CacheInstaller {
    @Override
    protected String getCacheKey() {
        return UserRestore.STORAGE_KEY;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException {
        return false;
    }
}

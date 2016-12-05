package org.commcare.resources.model.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.Reference;

/**
 * TODO: This should possibly just be replaced by a basic file installer along
 * with a reference for the login screen. We'll see.
 *
 * @author ctsims
 */
public class MediaInstaller extends BasicInstaller {

    public MediaInstaller() {

    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCarePlatform instance, boolean upgrade) throws UnresolvedResourceException {
        boolean result = super.install(r, location, ref, table, instance, upgrade);
        if (result) {
            table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
            return true;
        }
        return false;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }
}

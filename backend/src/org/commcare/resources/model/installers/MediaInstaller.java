/**
 *
 */
package org.commcare.resources.model.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
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

    public MediaInstaller(String path) {

    }

    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException {
        boolean result = super.install(r, location, ref, table, instance, upgrade);
        if (result) {
            table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInstaller#initialize()
     */
    public boolean initialize() throws ResourceInitializationException {
        //Tell the login screen where to get this?
        return true;
    }

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInstaller#requiresRuntimeInitialization()
     */
    public boolean requiresRuntimeInitialization() {
        return true;
    }
}

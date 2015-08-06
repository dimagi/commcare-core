/**
 *
 */
package org.commcare.resources.model.installers;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Used for any resources which:
 * 1) Are going to be stored in memory
 * 2) Possibly have derived resources
 * 3)
 *
 * NOTE: This functionality can probably be summed up into a
 * composite model, rather than an inheritance.
 *
 * @author ctsims
 */
public abstract class CacheInstaller<T extends Persistable> implements ResourceInstaller<CommCareInstance> {

    private IStorageUtility<T> cacheStorage;

    protected abstract String getCacheKey();

    protected IStorageUtility<T> storage() {
        if (cacheStorage == null) {
            cacheStorage = StorageManager.getStorage(getCacheKey());
        }
        return cacheStorage;
    }

    int cacheLocation;

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
     */
    public boolean initialize(CommCareInstance instance) throws ResourceInitializationException {
        //Suites don't need any local initialization (yet).
        return false;
    }

    /* (non-Javadoc)
     * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
     */
    public boolean requiresRuntimeInitialization() {
        //Nope.
        return false;
    }

    public abstract boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException;

    public boolean upgrade(Resource r) throws UnresolvedResourceException {
        //Don't need to do anything, since the resource is in the RMS already.
        throw new UnresolvedResourceException(r, "Attempt to upgrade installed resource suite");
    }

    public boolean uninstall(Resource r) {
        try {
            storage().remove(cacheLocation);
        } catch (IllegalArgumentException e) {
            //Already gone! Shouldn't need to fail.
        }
        return true;
    }

    public boolean unstage(Resource r, int newStatus) {
        //By default, shouldn't need to move anything.
        return true;
    }

    public boolean revert(Resource r, ResourceTable table) {
        //By default, shouldn't need to move anything.
        return true;
    }

    public int rollback(Resource r) {
        //This does nothing, since we don't do any upgrades/unstages
        return Resource.getCleanFlag(r.getStatus());
    }


    public void cleanup() {
        if (cacheStorage != null) {
            cacheStorage.close();
        }
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        cacheLocation = ExtUtil.readInt(in);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, cacheLocation);
    }


    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> resources) {
        return false;
    }

}

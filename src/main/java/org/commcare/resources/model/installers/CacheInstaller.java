package org.commcare.resources.model.installers;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
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
 *
 * @author ctsims
 */
public abstract class CacheInstaller<T extends Persistable> implements ResourceInstaller<CommCarePlatform> {

    private IStorageUtilityIndexed<T> cacheStorage;
    protected int cacheLocation;

    protected abstract String getCacheKey();

    protected IStorageUtilityIndexed<T> storage() {
        if (cacheStorage == null) {
            cacheStorage = StorageManager.getStorage(getCacheKey());
        }
        return cacheStorage;
    }

    @Override
    public abstract boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCarePlatform instance, boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException;

    @Override
    public boolean initialize(CommCarePlatform instance, boolean isUpgrade) {
        return false;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return false;
    }

    @Override
    public boolean upgrade(Resource r, CommCarePlatform instance) throws UnresolvedResourceException {
        //Don't need to do anything, since the resource is in the RMS already.
        throw new UnresolvedResourceException(r, "Attempt to upgrade installed resource suite");
    }

    @Override
    public boolean uninstall(Resource r) {
        try {
            storage().remove(cacheLocation);
        } catch (IllegalArgumentException e) {
            //Already gone! Shouldn't need to fail.
        }
        return true;
    }

    @Override
    public boolean unstage(Resource r, int newStatus) {
        //By default, shouldn't need to move anything.
        return true;
    }

    @Override
    public boolean revert(Resource r, ResourceTable table) {
        //By default, shouldn't need to move anything.
        return true;
    }

    @Override
    public int rollback(Resource r) {
        //This does nothing, since we don't do any upgrades/unstages
        return Resource.getCleanFlag(r.getStatus());
    }

    @Override
    public void cleanup() {
        if (cacheStorage != null) {
            cacheStorage.close();
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        cacheLocation = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, cacheLocation);
    }

    @Override
    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> resources) {
        return false;
    }
}

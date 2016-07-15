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
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * TODO: This is... not useful
 *
 * @author ctsims
 */
public class BasicInstaller implements ResourceInstaller<CommCareInstance> {

    @Override
    public boolean initialize(CommCareInstance instance, boolean isUpgrade) throws ResourceInitializationException {
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return false;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException {
        //If we have local resource authority, and the file exists, things are golden. We can just use that file.
        if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
            try {
                //If the file isn't there, not much we can do about it.
                return ref.doesBinaryExist();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
            //We need to download the resource, and store it locally. Either in the cache
            //(if no resource location is available) or in a local reference if one exists.
            InputStream incoming;
            try {
                incoming = ref.getStream();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (incoming == null) {
                //if it turns out there isn't actually a remote resource, bail.
                return false;
            }
            //TODO: Implement local cache code
            return false;
        }
        return false;
    }


    @Override
    public boolean upgrade(Resource r) throws UnresolvedResourceException {
        throw new RuntimeException("Basic Installer resources can't be marked as upgradable");
    }

    @Override
    public boolean uninstall(Resource r) throws UnresolvedResourceException {
        return true;
    }

    @Override
    public boolean unstage(Resource r, int newStatus) {
        return true;
    }

    @Override
    public boolean revert(Resource r, ResourceTable table) {
        return true;
    }

    @Override
    public int rollback(Resource r) {
        throw new RuntimeException("Basic Installer resources can't rolled back");
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {

    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
    }

    @Override
    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problems) {
        //Work by default
        return true;
    }
}

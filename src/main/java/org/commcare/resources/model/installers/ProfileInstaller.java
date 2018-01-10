package org.commcare.resources.model.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCarePlatform;
import org.commcare.xml.ProfileParser;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.Logger;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * @author ctsims
 */
public class ProfileInstaller extends CacheInstaller<Profile> {

    private static Hashtable<String, Profile> localTable;
    private boolean forceVersion;

    @SuppressWarnings("unused")
    public ProfileInstaller() {
        forceVersion = false;
    }

    public ProfileInstaller(boolean forceVersion) {
        this.forceVersion = forceVersion;
    }

    private Hashtable<String, Profile> getlocal() {
        if (localTable == null) {
            localTable = new Hashtable<>();
        }
        return localTable;
    }

    @Override
    public boolean initialize(CommCarePlatform instance, boolean isUpgrade) {
        //Certain properties may not have been able to set during install, so we'll make sure they're
        //set here.
        Profile p = storage(instance).read(cacheLocation);
        p.initializeProperties(instance, false);

        instance.setProfile(p);
        return true;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return true;
    }

    @Override
    protected String getCacheKey() {
        return Profile.STORAGE_KEY;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location,
                           Reference ref, ResourceTable table,
                           CommCarePlatform instance, boolean upgrade)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {
        //Install for the profile installer is a two step process. Step one is to parse the file and read the relevant data.
        //Step two is to actually install the resource if it needs to be (whether or not it should will be handled
        //by the resource table).

        InputStream incoming = null;
        //If we've already got the local copy, and the installer is marked as such, install and roll out.
        try {
            if (getlocal().containsKey(r.getRecordGuid()) && r.getStatus() == Resource.RESOURCE_STATUS_LOCAL) {
                Profile local = getlocal().get(r.getRecordGuid());
                installInternal(local, instance);
                table.commitCompoundResource(r, Resource.RESOURCE_STATUS_UPGRADE);
                localTable.remove(r.getRecordGuid());

                for (Resource child : table.getResourcesForParent(r.getRecordGuid())) {
                    table.commitCompoundResource(child, Resource.RESOURCE_STATUS_UNINITIALIZED);
                }
                return true;
            }

            //Otherwise we need to get the profile from its location, parse it out, and
            //set the relevant parameters.
            if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
                //If it's in the cache, we should just get it from there
                return false;
            } else {
                Profile p;
                try {
                    incoming = ref.getStream();
                    ProfileParser parser = new ProfileParser(incoming, instance, table, r.getRecordGuid(),
                            Resource.RESOURCE_STATUS_UNINITIALIZED, forceVersion);
                    if (Resource.RESOURCE_AUTHORITY_REMOTE == location.getAuthority()) {
                        parser.setMaximumAuthority(Resource.RESOURCE_AUTHORITY_REMOTE);
                    }
                    p = parser.parse();
                } catch (IOException e) {
                    if (e.getMessage() != null) {
                        Logger.log("resource", "IO Exception fetching profile: " + e.getMessage());
                    }
                    throw new UnreliableSourceException(r, e.getMessage());
                }

                //If we're upgrading we need to come back and see if the statuses need to change
                if (upgrade) {
                    getlocal().put(r.getRecordGuid(), p);
                    table.commitCompoundResource(r, Resource.RESOURCE_STATUS_LOCAL, p.getVersion());
                } else {
                    p.initializeProperties(instance, true);
                    installInternal(p, instance);
                    //TODO: What if this fails? Maybe we should be throwing exceptions...
                    table.commitCompoundResource(r, Resource.RESOURCE_STATUS_INSTALLED, p.getVersion());
                }

                return true;
            }
        } catch (InvalidStructureException e) {
            if (e.getMessage() != null) {
                Logger.log("resource", "Invalid profile structure: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        } catch (XmlPullParserException e) {
            if (e.getMessage() != null) {
                Logger.log("resource", "XML Parse exception fetching profile: " + e.getMessage());
            }
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

    private void installInternal(Profile profile, CommCarePlatform instance) {
        storage(instance).write(profile);
        cacheLocation = profile.getID();
    }

    @Override
    public boolean upgrade(Resource r, CommCarePlatform instance) throws UnresolvedResourceException {
        //TODO: Hm... how to do this property setting for reverting?

        Profile p;
        if (getlocal().containsKey(r.getRecordGuid())) {
            p = getlocal().get(r.getRecordGuid());
        } else {
            p = storage(instance).read(cacheLocation);
        }
        p.initializeProperties(instance, true);
        storage(instance).write(p);
        return true;
    }

    @Override
    public boolean unstage(Resource r, int newStatus, CommCarePlatform instance) {
        //Nothing to do. Cache location is clear.
        return true;
    }

    @Override
    public boolean revert(Resource r, ResourceTable table, CommCarePlatform instance) {
        //Possibly re-set this profile's default property setters.
        return true;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (localTable != null) {
            localTable.clear();
            localTable = null;
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        forceVersion = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeBool(out, forceVersion);
    }
}

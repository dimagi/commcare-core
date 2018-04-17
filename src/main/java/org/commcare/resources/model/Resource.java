package org.commcare.resources.model;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * <p>
 * Resources are records which resolve the location of data
 * definitions (Suites, Xforms, Images, etc), and keep track
 * of their status in the local environment. A Resource model
 * knows where certain resources definitions can be found, what
 * abstract resource those definitions are, a unique status about
 * whether that resource is installed or locally available,
 * and what installer it uses.</p>
 *
 * <p>
 * Resources are immutable and should be treated as such. The
 * abstract definition of a resource model is actually inside
 * of the Resource Table, and changes should be committed to
 * the table in order to change the resource.</p>
 *
 * <p>
 * As resources are installed into the local environment, their
 * status is updated to reflect that progress. The possible status
 * enumerations are:
 *
 * <ul>
 * <li>RESOURCE_STATUS_UNINITIALIZED - The resource has not yet been
 * evaluated by the the resource table.</li>
 * <li>RESOURCE_STATUS_LOCAL - The resource definition is locally present
 * and ready to be read and installed</li>
 * <li>RESOURCE_STATUS_INSTALLED - This resource is present locally and has
 * been installed. It is ready to be used.</li>
 * <li>RESOURCE_STATUS_UPGRADE - This resource definition has been read, and
 * the resource is present locally and ready to install, but a previous
 * version of it must be uninstalled first so its place can be taken.</li>
 * <li>RESOURCE_STATUS_DELETE - This resource is no longer needed and should
 * be uninstalled and its record removed.</li>
 * </ul>
 * </p>
 *
 * @author ctsims
 */
public class Resource implements Persistable, IMetaData {

    public static final String META_INDEX_RESOURCE_ID = "ID";
    public static final String META_INDEX_RESOURCE_GUID = "RGUID";
    public static final String META_INDEX_PARENT_GUID = "PGUID";
    public static final String META_INDEX_VERSION = "VERSION";

    public static final int RESOURCE_AUTHORITY_LOCAL = 0;
    public static final int RESOURCE_AUTHORITY_REMOTE = 1;
    public static final int RESOURCE_AUTHORITY_CACHE = 2;
    public static final int RESOURCE_AUTHORITY_RELATIVE = 4;
    public static final int RESOURCE_AUTHORITY_TEMPORARY = 8;

    // Completely Unprocessed
    public static final int RESOURCE_STATUS_UNINITIALIZED = 0;

    // Resource is in the local environment and ready to install
    public static final int RESOURCE_STATUS_LOCAL = 1;

    // Installed and ready to use
    public static final int RESOURCE_STATUS_INSTALLED = 4;

    // Resource is ready to replace an existing installed resource.
    public static final int RESOURCE_STATUS_UPGRADE = 8;

    // Resource is no longer needed in the local environment and can be
    // completely removed
    public static final int RESOURCE_STATUS_DELETE = 16;

    // Resource has been "unstaged" (won't necessarily work as an app
    // resource), but can be reverted to installed atomically.
    public static final int RESOURCE_STATUS_UNSTAGED = 17;

    // Resource is transitioning from installed to unstaged, and can be in any
    // interstitial state.
    public static final int RESOURCE_STATUS_INSTALL_TO_UNSTAGE = 18;

    // Resource is transitioning from unstaged to being installed
    public static final int RESOURCE_STATUS_UNSTAGE_TO_INSTALL = 19;

    // Resource is transitioning from being upgraded to being installed
    public static final int RESOURCE_STATUS_UPGRADE_TO_INSTALL = 20;

    // Resource is transitioning from being installed to being upgraded
    public static final int RESOURCE_STATUS_INSTALL_TO_UPGRADE = 21;

    public static final int RESOURCE_VERSION_UNKNOWN = -2;

    protected int recordId = -1;
    protected int version;
    protected int status;
    protected String id;
    protected Vector<ResourceLocation> locations;
    protected ResourceInstaller initializer;
    protected String guid;

    // Not sure if we want this persisted just yet...
    protected String parent;

    private String descriptor;

    /**
     * For serialization only
     */
    public Resource() {

    }

    /**
     * Creates a resource record identifying where a specific version of a resource
     * can be located.
     *
     * @param version    The version of the resource being defined.
     * @param id         A unique string identifying the abstract resource
     * @param locations  A set of locations from which this resource's definition
     *                   can be retrieved. Note that this vector is copied and should not be changed
     *                   after being passed in here.
     */
    public Resource(int version, String id, Vector<ResourceLocation> locations, String descriptor) {
        this.version = version;
        this.id = id;
        this.locations = locations;
        this.guid = PropertyUtils.genGUID(25);
        this.status = RESOURCE_STATUS_UNINITIALIZED;
        this.descriptor = descriptor;
    }

    /**
     * @return The locations where this resource's definition can be obtained.
     */
    public Vector<ResourceLocation> getLocations() {
        return locations;
    }

    /**
     * @return An enumerated ID identifying the status of this resource on
     * the local device.
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return The unique identifier for what resource this record offers the definition of.
     */
    public String getResourceId() {
        return id;
    }

    /**
     * @return A GUID that the resource table uses to identify this definition.
     */
    public String getRecordGuid() {
        return guid;
    }

    /**
     * @param parent The GUID of the resource record which has made this resource relevant
     *               for installation. This method should only be called by a resource table committing
     *               this resource record definition.
     */
    protected void setParentId(String parent) {
        this.parent = parent;
    }

    /**
     * @return True if this resource's relevance is derived from another resource. False
     * otherwise.
     */
    public boolean hasParent() {
        return !(parent == null || "".equals(parent));
    }

    /**
     * @return The GUID of the resource record which has made this resource relevant
     * for installation. This method should only be called by a resource table committing
     * this resource record definition.
     */
    public String getParentId() {
        return parent;
    }

    /**
     * @return The version of the resource that this record defines.
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version The version of the resource that this record defines. Can only be used
     *                to set the version if the current version is RESOURCE_VERSION_UNKOWN.
     */
    protected void setVersion(int version) {
        if (this.version == Resource.RESOURCE_VERSION_UNKNOWN) {
            this.version = version;
        }
    }

    /**
     * @param initializer Associates a ResourceInstaller with this resource record. This method
     *                    should only be called by a resource table committing this resource record definition.
     */
    public void setInstaller(ResourceInstaller initializer) {
        this.initializer = initializer;
    }

    /**
     * @return The installer which should be used to install the resource for this record.
     */
    public ResourceInstaller getInstaller() {
        return initializer;
    }

    /**
     * @param status The current status of this resource. Should only be called by the resource
     *               table.
     */
    protected void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getID() {
        return recordId;
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    /**
     * @param peer A resource record which defines the same resource as this record.
     * @return True if this record defines a newer version of the same resource as
     * peer, or if this resource generally is suspected to obsolete peer (if, for
     * instance this resource's version is yet unknown it will be assumed that it
     * is newer until it is.)
     */
    public boolean isNewer(Resource peer) {
        return version == RESOURCE_VERSION_UNKNOWN ||
                (peer.id.equals(this.id) && version > peer.getVersion());
    }

    /**
     * Take on all identifiers from the incoming
     * resouce, so as to replace it in a different table.
     */
    public void mimick(Resource source) {
        this.guid = source.guid;
        this.id = source.id;
        this.recordId = source.recordId;
        this.descriptor = source.descriptor;
    }

    public String getDescriptor() {
        if (descriptor == null) {
            return id;
        } else {
            return descriptor;
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.recordId = ExtUtil.readInt(in);
        this.version = ExtUtil.readInt(in);
        this.id = ExtUtil.readString(in);
        this.guid = ExtUtil.readString(in);
        this.status = ExtUtil.readInt(in);
        this.parent = ExtUtil.nullIfEmpty(ExtUtil.readString(in));

        locations = (Vector<ResourceLocation>)ExtUtil.read(in, new ExtWrapList(ResourceLocation.class), pf);
        this.initializer = (ResourceInstaller)ExtUtil.read(in, new ExtWrapTagged(), pf);
        this.descriptor = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeNumeric(out, version);
        ExtUtil.writeString(out, id);
        ExtUtil.writeString(out, guid);
        ExtUtil.writeNumeric(out, status);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(parent));

        ExtUtil.write(out, new ExtWrapList(locations));
        ExtUtil.write(out, new ExtWrapTagged(initializer));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(descriptor));
    }

    @Override
    public Object getMetaData(String fieldName) {
        switch (fieldName) {
            case META_INDEX_RESOURCE_ID:
                return id;
            case META_INDEX_RESOURCE_GUID:
                return guid;
            case META_INDEX_PARENT_GUID:
                return parent == null ? "" : parent;
            case META_INDEX_VERSION:
                return version;
        }
        throw new IllegalArgumentException("No Field w/name " + fieldName + " is relevant for resources");
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{META_INDEX_RESOURCE_ID, META_INDEX_RESOURCE_GUID, META_INDEX_PARENT_GUID, META_INDEX_VERSION};
    }

    public boolean isDirty() {
        return (getStatus() == Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE ||
                getStatus() == Resource.RESOURCE_STATUS_INSTALL_TO_UPGRADE ||
                getStatus() == Resource.RESOURCE_STATUS_UNSTAGE_TO_INSTALL ||
                getStatus() == Resource.RESOURCE_STATUS_UPGRADE_TO_INSTALL);
    }

    public static int getCleanFlag(int dirtyFlag) {
        // We actually will just push it forward by default, since this method
        // is used by things that can only be in the right state
        if (dirtyFlag == Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE) {
            return RESOURCE_STATUS_UNSTAGED;
        } else if (dirtyFlag == Resource.RESOURCE_STATUS_INSTALL_TO_UPGRADE) {
            return RESOURCE_STATUS_UPGRADE;
        } else if (dirtyFlag == Resource.RESOURCE_STATUS_UNSTAGE_TO_INSTALL) {
            return RESOURCE_STATUS_INSTALLED;
        } else if (dirtyFlag == Resource.RESOURCE_STATUS_UPGRADE_TO_INSTALL) {
            return RESOURCE_STATUS_INSTALLED;
        }
        return -1;
    }
}

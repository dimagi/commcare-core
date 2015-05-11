package org.commcare.resources.model;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;

import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.util.CommCareInstance;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;

/**
 * <p>A Resource Table maintains a set of Resource Records,
 * resolves dependencies between records, and provides hooks
 * for maintenance, updating, and initializing resources.</p>
 *
 * @author ctsims
 */
public class ResourceTable {

    // TODO: We have too many vectors here. It's lazy and incorrect. Everything
    // should be using iterators, not VECTORS;

    private IStorageUtilityIndexed storage;
    private InstallerFactory factory;

    // nothing here
    public final static int RESOURCE_TABLE_EMPTY = 0;
    // this is the table currently being used by the app
    public final static int RESOURCE_TABLE_INSTALLED = 1;
    // in any number of intermediate stages
    public final static int RESOURCE_TABLE_PARTIAL = 2;
    // this is the table constructed in order to do an upgrade --
    // means that it is not ready to upgrade the current table
    public final static int RESOURCE_TABLE_UPGRADE = 3;

    public static final int RESOURCE_TABLE_UNSTAGED = 4;
    public static final int RESOURCE_TABLE_UNCOMMITED = 5;

    /**
     * For Serialization Only!
     */
    public ResourceTable() {
    }

    public boolean isEmpty() {
        if (storage.getNumRecords() > 0) {
            return false;
        } else {
            return true;
        }
    }

    public static ResourceTable RetrieveTable(IStorageUtilityIndexed storage) {
        return RetrieveTable(storage, new InstallerFactory());
    }

    public static ResourceTable RetrieveTable(IStorageUtilityIndexed storage, InstallerFactory factory) {
        ResourceTable table = new ResourceTable();
        table.storage = storage;
        table.factory = factory;
        return table;
    }

    public int getTableReadiness() {
        // TODO: this is very hard to fully specify without doing assertions when preparing a
        // table about appropriate states

        boolean isFullyInstalled = true;
        boolean isEmpty = true;
        boolean unstaged = false;
        boolean upgrade = false;
        boolean dirty = false;

        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (r.getStatus() != Resource.RESOURCE_STATUS_INSTALLED) {
                isFullyInstalled = false;
            }
            if (r.getStatus() != Resource.RESOURCE_STATUS_UNINITIALIZED) {
                isEmpty = false;
            }

            if (r.getStatus() == Resource.RESOURCE_STATUS_UNSTAGED) {
                unstaged = true;
            }
            if (r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE) {
                upgrade = true;
            }
            if (r.isDirty()) {
                dirty = true;
            }
        }

        if (dirty) {
            return RESOURCE_TABLE_UNCOMMITED;
        }
        if (isEmpty) {
            return RESOURCE_TABLE_EMPTY;
        }
        if (isFullyInstalled) {
            return RESOURCE_TABLE_INSTALLED;
        }
        if (unstaged) {
            return RESOURCE_TABLE_UNSTAGED;
        }
        if (upgrade) {
            return RESOURCE_TABLE_UPGRADE;
        }

        return RESOURCE_TABLE_PARTIAL;
    }

    public InstallerFactory getInstallers() {
        return factory;
    }

    public void removeResource(Resource resource) {
        storage.remove(resource);
    }

    public void addResource(Resource resource, ResourceInstaller initializer, String parentId, int status) throws StorageFullException {
        resource.setInstaller(initializer);
        resource.setParentId(parentId);
        addResource(resource, status);
    }

    public void addResource(Resource resource, ResourceInstaller initializer, String parentId) throws StorageFullException {
        addResource(resource, initializer, parentId, Resource.RESOURCE_STATUS_UNINITIALIZED);
    }

    public void addResource(Resource resource, int status) throws StorageFullException {
        Vector<Integer> existing = storage.getIDsForValue(Resource.META_INDEX_RESOURCE_ID, resource.getResourceId());
        for (Integer i : existing) {
            Resource r = (Resource)storage.read(i.intValue());
            // this resource is already here! No worries
            return;
        }

        resource.setStatus(status);
        try {
            //TODO: Check if it exists?
            if (resource.getID() != -1) {
                //Assume that we're going cross-table, so we need a new RecordId.
                resource.setID(-1);

                //Check to make sure that there's no existing GUID for this record.
                if (getResourceWithGuid(resource.getRecordGuid()) != null) {
                    throw new RuntimeException("Why are you adding a record that already exists? Huh?");
                }
            }
            storage.write(resource);
        } catch (StorageFullException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Vector<Resource> getResourcesForParent(String parent) {
        Vector<Resource> v = new Vector<Resource>();
        for (Enumeration en = storage.getIDsForValue(Resource.META_INDEX_PARENT_GUID, parent).elements(); en.hasMoreElements(); ) {
            Resource r = (Resource)storage.read(((Integer)en.nextElement()).intValue());
            v.addElement(r);
        }
        return v;
    }

    public Resource getResourceWithId(String id) {
        try {
            return (Resource)storage.getRecordForValue(Resource.META_INDEX_RESOURCE_ID, id);
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    public Resource getResourceWithGuid(String guid) {
        try {
            return (Resource)storage.getRecordForValue(Resource.META_INDEX_RESOURCE_GUID, guid);
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    private Vector<Resource> GetResources() {
        Vector<Resource> v = new Vector<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            v.addElement(r);
        }
        return v;
    }

    private Vector<Resource> GetResources(int status) {
        Vector<Resource> v = new Vector<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (r.getStatus() == status) {
                v.addElement(r);
            }
        }
        return v;
    }

    private Stack<Resource> GetResourceStack() {
        Stack<Resource> v = new Stack<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            v.push(r);
        }
        return v;
    }

    private Stack<Resource> GetResourceStack(int status) {
        Stack<Resource> v = new Stack<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (r.getStatus() == status) {
                v.push(r);
            }
        }
        return v;
    }


    /**
     * Get stored resources that are unready for installation, that is, not of
     * installed, upgrade, or pending status.
     *
     * Resources that are:
     *  - installed don't need anything
     *  - marked as ready for upgrade are ready
     *  - marked as pending aren't capable of installation yet
     *
     * @return Stack of resource records that aren't ready for installation
     */
    private Stack<Resource> getUnreadyResrouces() {
        Stack<Resource> v = new Stack<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (r.getStatus() != Resource.RESOURCE_STATUS_INSTALLED &&
                    r.getStatus() != Resource.RESOURCE_STATUS_UPGRADE &&
                    r.getStatus() != Resource.RESOURCE_STATUS_PENDING) {
                v.push(r);
            }
        }
        return v;
    }

    public boolean isReady() {
        return getUnreadyResrouces().size() == 0;
    }

    public void commit(Resource r, int status, int version) throws UnresolvedResourceException {
        if (r.getVersion() == Resource.RESOURCE_VERSION_UNKNOWN) {
            // Try to update the version.
            r.setVersion(version);
        } else {
            // Otherwise, someone screwed up
        }
        commit(r, status);
    }

    public void commit(Resource r, int status) {
        r.setStatus(status);
        commit(r);
    }

    public void commit(Resource r) {
        storage.write(r);
    }

    /**
     * Rolls back uncommitted resources from dirty states
     */
    public void rollbackCommits() {
        Stack<Resource> s = this.GetResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.isDirty()) {
                this.commit(r, r.getInstaller().rollback(r));
            }
        }
    }

    /**
     * @param r
     * @param invalid
     * @param upgrade
     * @param instance
     * @param master
     * @throws UnresolvedResourceException       Raised when no definitions for
     *                                           resource 'r' can't be found
     * @throws UnfullfilledRequirementsException
     */
    private void checkForLocalResourceStatus(Resource r,
                                             Vector<Reference> invalid,
                                             boolean upgrade,
                                             CommCareInstance instance,
                                             ResourceTable master)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {

        // TODO: Possibly check if resource status is local and proceeding to
        // skip this huge (although in reality like one step) chunk

        UnreliableSourceException theFailure = null;
        boolean handled = false;

        for (ResourceLocation location : r.getLocations()) {
            if (handled) {
                break;
            }
            if (location.isRelative()) {
                for (Reference ref : explodeReferences(location, r, this, master)) {
                    if (!(location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL && invalid.contains(ref))) {
                        try {
                            handled = installResource(r, location, ref, this,
                                    instance, upgrade);
                        } catch (UnreliableSourceException use) {
                            theFailure = use;
                        }
                        if (handled) {
                            break;
                        }
                    }
                }
            } else {
                try {
                    handled = installResource(r, location,
                            ReferenceManager._().DeriveReference(location.getLocation()),
                            this, instance, upgrade);
                    if (handled) {
                        break;
                    }
                } catch (InvalidReferenceException ire) {
                    ire.printStackTrace();
                    // Continue until no resources can be found.
                } catch (UnreliableSourceException use) {
                    theFailure = use;
                }
            }
        }

        if (!handled) {
            if (theFailure == null) {
                // no particular failure to point our finger at.
                throw new UnresolvedResourceException(r,
                        "No external or local definition could be found for resource " +
                                r.getResourceId());
            } else {
                // Expose the lossy failure rather than the generic one
                throw theFailure;
            }
        }
    }

    /**
     * Makes all of this table's resources available.
     *
     * @param master   The global resource to prepare against. Used to establish whether resources need to be fetched
     *                 remotely
     * @param instance The instance to prepare against
     * @throws UnresolvedResourceException       If a resource could not be identified and is required
     * @throws UnfullfilledRequirementsException If some resources are incompatible with the current version of CommCare
     */
    public void prepareResources(ResourceTable master, CommCareInstance instance)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {
        this.prepareResources(master, instance, null);
    }

    /**
     * Makes some (or all) of the table's resources available
     *
     * @param master       The global resource to prepare against. Used to
     *                     establish whether resources need to be fetched remotely
     * @param instance     The instance to prepare against
     * @param toInitialize The ID of a single resource after which the table
     *                     preparation can stop.
     * @throws UnresolvedResourceException       If a resource could not be identified and is required
     * @throws UnfullfilledRequirementsException If some resources are incompatible with the current version of CommCare
     */
    public void prepareResources(ResourceTable master, CommCareInstance instance, String toInitialize)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {

        boolean idNeedsInitialization = true;
        if (toInitialize != null) {
            Resource res = this.getResourceWithId(toInitialize);
            if (res != null && res.getStatus() != Resource.RESOURCE_STATUS_UNINITIALIZED) {
                idNeedsInitialization = false;
            }
        }

        Stack<Resource> v = getUnreadyResrouces();

        if (idNeedsInitialization) {
            while (!v.isEmpty()) {
                Resource r = v.pop();
                boolean upgrade = false;

                Vector<Reference> invalid = new Vector<Reference>();

                // All operations regarding peers and master table
                if (master != null) {
                    Resource peer = master.getResourceWithId(r.getResourceId());
                    if (peer != null) {
                        // TODO: For now we're assuming that Versions greater
                        // than the current are always acceptable
                        if (!r.isNewer(peer)) {
                            // This resource doesn't need to be updated, copy
                            // the exisitng resource into this table
                            peer.mimick(r);
                            commit(peer, Resource.RESOURCE_STATUS_INSTALLED);
                            continue;
                        } else {
                            upgrade = true;
                        }
                        invalid = ResourceTable.explodeLocalReferences(peer, master);
                    }
                }

                checkForLocalResourceStatus(r, invalid, upgrade, instance, master);

                if (stateListener != null) {
                    stateListener.resourceStateUpdated(this);
                }

                v = getUnreadyResrouces();
            }
        }

        if (toInitialize != null) {
            // We will need to run  a full init later, so we can skip the next step
            return;
        }

        // TODO: Nothing uses this status, really. Should this go away?
        // Wipe out any resources which are still pending. If they weren't updated by their
        // parent, they aren't relevant.
        for (Resource stillPending : GetResources(Resource.RESOURCE_STATUS_PENDING)) {
            this.removeResource(stillPending);
        }
    }

    /**
     * This just calls the resource's installer directly, but also handles the
     * logic around attempting retries if applicable
     *
     * @throws UnfullfilledRequirementsException *
     */
    private boolean installResource(Resource r, ResourceLocation location,
                                    Reference ref, ResourceTable table,
                                    CommCareInstance instance, boolean upgrade)
            throws UnresolvedResourceException, UnfullfilledRequirementsException {
        UnreliableSourceException aFailure = null;

        for (int i = 0; i < this.numberOfLossyRetries + 1; ++i) {
            try {
                return r.getInstaller().install(r, location, ref, table, instance, upgrade);
            } catch (UnreliableSourceException use) {
                aFailure = use;
                Logger.log("install", "Potentially lossy install attempt # " +
                        (i + 1) + " of " + (numberOfLossyRetries + 1) +
                        " unsuccessful from: " + ref.getURI() + "|" +
                        use.getMessage());
            }
        }

        if (aFailure != null) {
            throw aFailure;
        }

        return false;
    }

    /**
     * Prepares this table to be replaced by the incoming table.
     *
     * All conflicting resources from this table will be unstaged so as to not conflict with the
     * incoming resources. Once the incoming table is fully installed, this table's resources
     * can then be fully removed where relevant.
     *
     * @param incoming
     * @return True if this table was prepared and the incoming table can be fully installed. False
     * if something is this table couldn't be unstaged.
     * @throws UnresolvedResourceException
     */
    public boolean upgradeTable(ResourceTable incoming) throws UnresolvedResourceException {
        if (!incoming.isReady()) {
            return false;
        }

        // Everything incoming should be marked either ready or upgrade. Upgrade elements
        // should result in their counterpart in this table being unstaged (which can be
        // reverted).

        Stack<Resource> resources = incoming.GetResourceStack();
        while (!resources.isEmpty()) {
            Resource r = resources.pop();
            Resource peer = this.getResourceWithId(r.getResourceId());
            if (peer == null) {
                this.addResource(r, Resource.RESOURCE_STATUS_INSTALLED);
            } else {
                if (r.isNewer(peer)) {
                    // Mark as being ready to transition
                    this.commit(peer, Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE);

                    if (!peer.getInstaller().unstage(peer, Resource.RESOURCE_STATUS_UNSTAGED)) {
                        // TODO: revert this resource table!
                        throw new UnresolvedResourceException(peer, "Couldn't make room for new resource " + r.getResourceId() + ", upgrade aborted");
                    } else {
                        // done
                        commit(peer, Resource.RESOURCE_STATUS_UNSTAGED);
                    }

                    if (r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE) {
                        incoming.commit(r, Resource.RESOURCE_STATUS_UPGRADE_TO_INSTALL);
                        if (r.getInstaller().upgrade(r)) {
                            incoming.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
                        } else {
                            System.out.println("Failed to upgrade resource: " + r.getDescriptor());
                            // REVERT!
                            return false;
                        }
                    }
                }
                if (peer.getVersion() == r.getVersion()) {
                    // Same resource. Don't do anything with it, it has no
                    // children, so ID's don't need to change.
                    // Technically resource locations could change, worth thinking
                    // about for the future.
                }
            }
            r = null;
        }

        return true;
    }

    public void flagForDeletions(ResourceTable replacement) {
        Stack<Resource> s = this.GetResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();
            Resource peer = replacement.getResourceWithId(r.getResourceId());

            // If this resource is no longer relevant
            if (peer == null) {
                this.commit(r, Resource.RESOURCE_STATUS_DELETE);
                continue;
            }

            // If this resource has been replaced
            if (r.getStatus() == Resource.RESOURCE_STATUS_UNSTAGED) {
                this.commit(r, Resource.RESOURCE_STATUS_DELETE);
            }
        }
    }


    /**
     * Called on a table to restage any unstaged resources.
     *
     * @param incoming The table which unstaged this table's resources
     */
    public void repairTable(ResourceTable incoming) {
        Stack<Resource> s = this.GetResourceStack(Resource.RESOURCE_STATUS_UNSTAGED);
        while (!s.isEmpty()) {
            Resource resource = s.pop();

            if (incoming != null) {
                // See if there's a competing resource
                Resource peer = incoming.getResourceWithId(resource.getResourceId());

                // If there is, and it's been installed, unstage it to make room again
                if (peer != null && peer.getStatus() == Resource.RESOURCE_STATUS_INSTALLED) {
                    incoming.commit(peer, Resource.RESOURCE_STATUS_INSTALL_TO_UPGRADE);
                    // TODO: Is there anything we can do about this? Shouldn't it be an exception?
                    if (!peer.getInstaller().unstage(peer, Resource.RESOURCE_STATUS_UPGRADE)) {
                        // TODO: IF there are errors here, signal that the incoming table
                        // should just be wiped out. It's not in acceptable shape
                    } else {
                        incoming.commit(peer, Resource.RESOURCE_STATUS_UPGRADE);
                    }
                }
            }

            // Way should be clear.
            this.commit(resource, Resource.RESOURCE_STATUS_UNSTAGE_TO_INSTALL);
            if (resource.getInstaller().revert(resource, this)) {
                this.commit(resource, Resource.RESOURCE_STATUS_INSTALLED);
            }
        }
    }

    /**
     * Complete the uninstallation of a table that has been overwritten.
     *
     * This method is the final step in an update, after this table has
     * already been moved to a placeholder table and been evaluated for
     * what resources are no longer necessary.
     *
     * If this table encounters any problems it will not intentionally
     * throw errors, assuming that it's preferable to leave data unremoved
     * rather than breaking the app.
     */
    public void completeUninstall() {
        cleanup();
        Stack<Resource> s = this.GetResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.getStatus() == Resource.RESOURCE_STATUS_DELETE) {
                try {
                    r.getInstaller().uninstall(r);
                } catch (Exception e) {
                    Logger.log("resources", "Error uninstalling resource " + r.getRecordGuid() + ". " + e.getMessage());
                }
            }
        }

        storage.removeAll();
    }


    /**
     * Copy all of this table's resource records to the (empty) table provided.
     *
     * @throws IllegalArgumentException If incoming table is not empty
     */
    public void copyToTable(ResourceTable newTable) throws IllegalArgumentException {
        if (!newTable.isEmpty()) {
            throw new IllegalArgumentException("Can't copy into a table with data in it!");
        }

        // Copy over all of our resources to the new table
        for (Resource r : this.GetResources()) {
            r.setID(-1);
            newTable.commit(r);
        }
    }

    public String toString() {
        String output = "";
        int ml = 0;
        for (Resource r : GetResources()) {
            String line = "| " + r.getResourceId() + " | " + r.getVersion() + " | " + getStatus(r.getStatus()) + " |\n";
            output += line;
            if (line.length() > ml) {
                ml = line.length();
            }
        }
        String cap = "";
        for (int i = 0; i < ml; ++i) {
            cap += "-";
        }
        return cap + "\n" + output + cap + "\n";
    }

    public static String getStatus(int status) {
        switch (status) {
            case Resource.RESOURCE_STATUS_UNINITIALIZED:
                return "Uninitialized";
            case Resource.RESOURCE_STATUS_LOCAL:
                return "Local";
            case Resource.RESOURCE_STATUS_PENDING:
                return "Pending other Resource";
            case Resource.RESOURCE_STATUS_INSTALLED:
                return "Installed";
            case Resource.RESOURCE_STATUS_UPGRADE:
                return "Ready for Upgrade";
            case Resource.RESOURCE_STATUS_DELETE:
                return "Flagged for Deletion";
            case Resource.RESOURCE_STATUS_UNSTAGED:
                return "Unstaged";
            case Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE:
                return "Install->Unstage (dirty)";
            case Resource.RESOURCE_STATUS_INSTALL_TO_UPGRADE:
                return "Install->Upgrade (dirty)";
            case Resource.RESOURCE_STATUS_UNSTAGE_TO_INSTALL:
                return "Unstage->Install (dirty)";
            case Resource.RESOURCE_STATUS_UPGRADE_TO_INSTALL:
                return "Upgrade->Install (dirty)";
            default:
                return "Unknown";
        }
    }

    /**
     * Destroy this table, but leave any of the files which are installed untouched.
     * This is useful after an upgrade if this is the temp table.
     */
    public void destroy() {
        cleanup();
        storage.removeAll();
    }

    /**
     * Destroy this table, and also try very hard to remove any files installed by it. This
     * is important for rolling back botched upgrades without leaving their files around.
     */
    public void clear() {
        cleanup();
        Stack<Resource> s = this.GetResourceStack();
        int count = 0;
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE) {
                try {
                    r.getInstaller().uninstall(r);
                    count++;
                } catch (UnresolvedResourceException e) {
                    // already gone!
                }
            }
        }
        if (count > 0) {
            Logger.log("Resource", "Cleaned up " + count + " records from table");
        }

        storage.removeAll();
    }

    private void cleanup() {
        for (Resource r : GetResources()) {
            r.getInstaller().cleanup();
        }
    }

    public void initializeResources(CommCareInstance instance) throws ResourceInitializationException {
        // HHaaaacckkk. (Some properties cannot be handled until after others
        // TODO: Replace this with some sort of sorted priority queue.
        Vector<ResourceInstaller> lateInit = new Vector<ResourceInstaller>();

        for (Resource r : this.GetResources()) {
            ResourceInstaller i = r.getInstaller();
            if (i.requiresRuntimeInitialization()) {
                if (i instanceof ProfileInstaller) {
                    lateInit.addElement(i);
                } else {
                    i.initialize(instance);
                }
            }
        }
        for (ResourceInstaller i : lateInit) {
            i.initialize(instance);
        }
    }

    /**
     * Find all absolute paths for a resource's various locations.
     *
     * @param r resource for which local location references are being gathered
     * @param t table to look-up the resource's parents in
     * @return all local references a resource's potential locations
     */
    private static Vector<Reference> explodeLocalReferences(Resource r, ResourceTable t) {
        Vector<Reference> ret = new Vector<Reference>();

        for (ResourceLocation location : r.getLocations()) {
            if (location.isRelative()) {
                if (r.hasParent()) {
                    Resource parent = t.getResourceWithGuid(r.getParentId());
                    if (parent != null) {
                        // Get local references for the parent resource's
                        // locations
                        Vector<Reference> parentRefs = explodeLocalReferences(parent, t);

                        for (Reference context : parentRefs) {
                            // contextualize the location ref in terms of the
                            // multiple refs pointing to different locations
                            // for the parent resource
                            try {
                                ret.addElement(ReferenceManager._().DeriveReference(location.getLocation(), context));
                            } catch (InvalidReferenceException ire) {
                                ire.printStackTrace();
                            }
                        }
                    }
                }
            } else if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
                try {
                    ret.addElement(ReferenceManager._().DeriveReference(location.getLocation()));
                } catch (InvalidReferenceException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    /**
     * @param location
     * @param r
     * @param t
     * @param m
     * @return
     */
    private static Vector<Reference> explodeReferences(ResourceLocation location,
                                                       Resource r,
                                                       ResourceTable t,
                                                       ResourceTable m) {
        int type = location.getAuthority();
        Vector<Reference> ret = new Vector<Reference>();

        if (r.hasParent()) {
            Resource parent = t.getResourceWithGuid(r.getParentId());

            // If the local table doesn't have the parent ref, try the master
            if (parent == null && m != null) {
                parent = m.getResourceWithGuid(r.getParentId());
            }
            if (parent != null) {
                // Get all local references for the parent
                Vector<Reference> parentRefs = explodeAllReferences(type, parent, t, m);
                for (Reference context : parentRefs) {
                    try {
                        ret.addElement(ReferenceManager._().DeriveReference(location.getLocation(), context));
                    } catch (InvalidReferenceException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ret;
    }

    private static Vector<Reference> explodeAllReferences(int type, Resource r, ResourceTable t, ResourceTable m) {
        Vector<ResourceLocation> locations = r.getLocations();
        Vector<Reference> ret = new Vector<Reference>();
        for (ResourceLocation location : locations) {
            if (location.getAuthority() == type) {
                if (location.isRelative()) {
                    if (r.hasParent()) {
                        Resource parent = t.getResourceWithGuid(r.getParentId());

                        // If the local table doesn't have the parent ref, try the master
                        if (parent == null) {
                            parent = m.getResourceWithGuid(r.getParentId());
                        }
                        if (parent != null) {
                            // Get all local references for the parent
                            Vector<Reference> parentRefs = explodeAllReferences(type, parent, t, m);
                            for (Reference context : parentRefs) {
                                try {
                                    ret.addElement(ReferenceManager._().DeriveReference(location.getLocation(), context));
                                } catch (InvalidReferenceException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    try {
                        ret.addElement(ReferenceManager._().DeriveReference(location.getLocation()));
                    } catch (InvalidReferenceException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ret;
    }

    public void verifyInstallation(Vector<MissingMediaException> problems) {
        Vector<Resource> resources = GetResources();
        int total = resources.size();
        int count = 0;
        for (Resource r : resources) {
            r.getInstaller().verifyInstallation(r, problems);
            count++;
            if (stateListener != null) {
                stateListener.incrementProgress(count, total);
            }
        }
    }

    TableStateListener stateListener = null;

    public void setStateListener(TableStateListener listener) {
        this.stateListener = listener;
    }

    int numberOfLossyRetries = 3;

    /**
     * Sets the number of attempts this table will make to install against resources which
     * fail on lossy (IE: Network) channels.
     *
     * @param number The number of attempts to make per resource. Must be at least 0
     */
    public void setNumberOfRetries(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Can't have less than 0 retries");
        }
        this.numberOfLossyRetries = number;
    }


}

package org.commcare.resources.model;

import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.Vector;

/**
 * A Resource Table maintains a set of Resource Records,
 * resolves dependencies between records, and provides hooks
 * for maintenance, updating, and initializing resources.
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

    private TableStateListener stateListener = null;
    private InstallCancelled cancellationChecker = null;
    private InstallStatsLogger installStatsLogger = null;

    private int numberOfLossyRetries = 3;

    /**
     * For Serialization Only!
     */
    public ResourceTable() {
    }

    public boolean isEmpty() {
        return storage.getNumRecords() <= 0;
    }

    public static ResourceTable RetrieveTable(IStorageUtilityIndexed storage) {
        return RetrieveTable(storage, new InstallerFactory());
    }

    public static ResourceTable RetrieveTable(IStorageUtilityIndexed storage,
                                              InstallerFactory factory) {
        ResourceTable table = new ResourceTable();
        table.storage = storage;
        table.factory = factory;
        return table;
    }

    public int getTableReadiness() {
        // TODO: this is very hard to fully specify without doing assertions
        // when preparing a table about appropriate states

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

    public void addResource(Resource resource, ResourceInstaller initializer,
                            String parentId, int status) {
        resource.setInstaller(initializer);
        resource.setParentId(parentId);
        addResource(resource, status);
    }

    public void addResource(Resource resource, ResourceInstaller initializer,
                            String parentId) {
        addResource(resource, initializer, parentId, Resource.RESOURCE_STATUS_UNINITIALIZED);
    }

    public void addResource(Resource resource, int status) {
        if (resourceDoesntExist(resource)) {
            addResourceInner(resource, status);
        } else {
            Logger.log("Resource", "Trying to add an already existing resource: " + resource.getResourceId());
        }
    }
    private boolean resourceDoesntExist(Resource resource) {
        return storage.getIDsForValue(Resource.META_INDEX_RESOURCE_ID, resource.getResourceId()).size() == 0;
    }

    private void addResourceInner(Resource resource, int status) {
        resource.setStatus(status);
        if (resource.getID() != -1) {
            // Assume that we're going cross-table, so we need a new
            // RecordId.
            resource.setID(-1);

            // Check to make sure that there's no existing GUID for
            // this record.
            if (getResourceWithGuid(resource.getRecordGuid()) != null) {
                throw new RuntimeException("This resource record already exists.");
            }
        }

        try {
            storage.write(resource);
        } catch (StorageFullException e) {
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

    /**
     * Get the all the resources in this table's storage.
     */
    private Vector<Resource> getResources() {
        Vector<Resource> v = new Vector<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            v.addElement(r);
        }
        return v;
    }

    /**
     * Get the resources in this table's storage that have a given status.
     */
    private Vector<Resource> getResourcesWithStatus(int status) {
        Vector<Resource> v = new Vector<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (r.getStatus() == status) {
                v.addElement(r);
            }
        }
        return v;
    }

    /**
     * Get the all the resources in this table's storage.
     */
    private Stack<Resource> getResourceStack() {
        Stack<Resource> v = new Stack<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            v.push(r);
        }
        return v;
    }

    /**
     * Get the resources in this table's storage that have a given status.
     */
    private Stack<Resource> getResourceStackWithStatus(int status) {
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
     * - installed don't need anything
     * - marked as ready for upgrade are ready
     * - marked as pending aren't capable of installation yet
     *
     * @return Stack of resource records that aren't ready for installation
     */
    private Stack<Resource> getUnreadyResources() {
        Stack<Resource> v = new Stack<Resource>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (r.getStatus() != Resource.RESOURCE_STATUS_INSTALLED &&
                    r.getStatus() != Resource.RESOURCE_STATUS_UPGRADE) {
                v.push(r);
            }
        }
        return v;
    }

    /**
     * Are all the resources ready to be installed or have already been
     * installed?
     */
    public boolean isReady() {
        return getUnreadyResources().size() == 0;
    }

    public void commit(Resource r, int status, int version) throws UnresolvedResourceException {
        if (r.getVersion() == Resource.RESOURCE_VERSION_UNKNOWN) {
            // Try to update the version.
            r.setVersion(version);
        } else {
            // Otherwise, someone screwed up
            Logger.log("Resource", "committing a resource with a known version.");
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
        Stack<Resource> s = this.getResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.isDirty()) {
                this.commit(r, r.getInstaller().rollback(r));
            }
        }
    }

    /**
     * Install a resource by looping through its locations stopping at first
     * successful install.
     *
     * @param r        Resource to install
     * @param invalid  out-of-date locations to be avoided during resource
     *                 installation
     * @param upgrade  Has an older version of the resource been installed?
     * @param instance The CommCare instance (specific profile and version) to
     *                 prepare against
     * @param master   Backup resource table to look-up resources not found in
     *                 the current table
     * @throws UnresolvedResourceException       Raised when no definitions for
     *                                           resource 'r' can't be found
     * @throws UnfullfilledRequirementsException
     */
    private void findResourceLocationAndInstall(Resource r,
                                                Vector<Reference> invalid,
                                                boolean upgrade,
                                                CommCareInstance instance,
                                                ResourceTable master)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {

        // TODO: Possibly check if resource status is local and proceeding to
        // skip this huge (although in reality like one step) chunk

        UnreliableSourceException theFailure = null;
        boolean handled = false;

        for (ResourceLocation location : r.getLocations()) {
            if (handled) {
                break;
            }
            if (location.isRelative()) {
                for (Reference ref : gatherLocationsRefs(location, r, this, master)) {
                    if (!(location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL && invalid.contains(ref))) {
                        try {
                            handled = installResource(r, location, ref, this,
                                    instance, upgrade);
                        } catch (UnreliableSourceException use) {
                            theFailure = use;
                        }
                        if (handled) {
                            recordSuccess(r);
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
                        recordSuccess(r);
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
     * @param master   The global resource to prepare against. Used to
     *                 establish whether resources need to be fetched remotely
     * @param instance The instance (version and profile) to prepare against
     * @throws UnresolvedResourceException       If a resource could not be
     *                                           identified and is required
     * @throws UnfullfilledRequirementsException If some resources are
     *                                           incompatible with the current
     *                                           version of CommCare
     */
    public void prepareResources(ResourceTable master, CommCareInstance instance)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {

        Stack<Resource> unreadyResources = getUnreadyResources();

        // install all unready resources.
        while (!unreadyResources.isEmpty()) {
            for (Resource r : unreadyResources) {
                prepareResource(master, instance, r);
            }
            // Installing resources may have exposed more unready resources
            // that need installing.
            unreadyResources = getUnreadyResources();
        }
    }

    /**
     * Makes all resources available until toInitialize is encountered.
     *
     * @param master       The global resource to prepare against. Used to
     *                     establish whether resources need to be fetched remotely
     * @param instance     The instance (version and profile) to prepare against
     * @param toInitialize The ID of a single resource after which the table
     *                     preparation can stop.
     * @throws UnresolvedResourceException       Required resource couldn't be
     *                                           identified
     * @throws UnfullfilledRequirementsException resource(s) incompatible with
     *                                           current CommCare version
     */
    public void prepareResourcesUpTo(ResourceTable master,
                                     CommCareInstance instance,
                                     String toInitialize)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {

        Vector<Resource> unreadyResources = getUnreadyResources();

        // install unready resources, until toInitialize has been installed.
        while (isResourceUninitialized(toInitialize) && !unreadyResources.isEmpty()) {
            for (Resource r : unreadyResources) {
                prepareResource(master, instance, r);
            }
            // Installing resources may have exposed more unready resources
            // that need installing.
            unreadyResources = getUnreadyResources();
        }
    }

    private void prepareResource(ResourceTable master, CommCareInstance instance,
                                 Resource r)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {
        boolean upgrade = false;

        Vector<Reference> invalid = new Vector<Reference>();

        if (master != null) {
            // obtain resource peer by looking up the current resource
            // in the master table
            Resource peer = master.getResourceWithId(r.getResourceId());
            if (peer != null) {
                // TODO: For now we're assuming that Versions greater
                // than the current are always acceptable
                if (!r.isNewer(peer)) {
                    // This resource doesn't need to be updated, copy
                    // the existing resource into this table
                    peer.mimick(r);
                    commit(peer, Resource.RESOURCE_STATUS_INSTALLED);
                    return;
                }

                // resource is newer than master version, so invalidate
                // old local resource locations.
                upgrade = true;
                invalid = ResourceTable.gatherResourcesLocalRefs(peer, master);
            }
        }

        findResourceLocationAndInstall(r, invalid, upgrade, instance, master);

        if (stateListener != null) {
            stateListener.resourceStateUpdated(this);
        }
    }

    private boolean isResourceUninitialized(String resourceId) {
        Resource res = this.getResourceWithId(resourceId);
        return ((res == null) ||
                (res.getStatus() == Resource.RESOURCE_STATUS_UNINITIALIZED));
    }

    /**
     * Call the resource's installer, handling the logic around attempting
     * retries.
     *
     * @return Did the resource install successfully?
     * @throws UnfullfilledRequirementsException
     */
    private boolean installResource(Resource r, ResourceLocation location,
                                    Reference ref, ResourceTable table,
                                    CommCareInstance instance, boolean upgrade)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {
        UnreliableSourceException aFailure = null;

        for (int i = 0; i < this.numberOfLossyRetries + 1; ++i) {
            abortIfInstallCancelled(r);
            try {
                return r.getInstaller().install(r, location, ref, table, instance, upgrade);
            } catch (UnreliableSourceException use) {
                recordFailure(r, use);
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

    private void abortIfInstallCancelled(Resource r) throws InstallCancelledException {
        if (cancellationChecker != null && cancellationChecker.wasInstallCancelled()) {
            InstallCancelledException installException =
                new InstallCancelledException("Installation/upgrade was cancelled while processing " + r.getResourceId());
            recordFailure(r, installException);
            throw installException;
        }
    }

    private void recordFailure(Resource resource, Exception e) {
        if (installStatsLogger != null) {
            installStatsLogger.recordResourceInstallFailure(resource.getResourceId(), e);
        }
    }

    private void recordSuccess(Resource resource) {
        if (installStatsLogger != null) {
            installStatsLogger.recordResourceInstallSuccess(resource.getResourceId());
        }
    }

    /**
     * Prepare this table to be replaced by the incoming table, and incoming
     * table to replace it.
     *
     * All conflicting resources from this table will be unstaged so as to not
     * conflict with the incoming resources. Once the incoming table is fully
     * installed, this table's resources can then be fully removed where
     * relevant.
     *
     * @param incoming Table for which resource upgrades are applied
     * @return True if this table was prepared and the incoming table can be
     * fully installed. False if something is this table couldn't be unstaged.
     * @throws UnresolvedResourceException
     */
    public boolean upgradeTable(ResourceTable incoming) throws UnresolvedResourceException {
        if (!incoming.isReady()) {
            return false;
        }

        // Everything incoming should be marked either ready or upgrade.
        // Upgrade elements should result in their counterpart in this table
        // being unstaged (which can be reverted).

        Stack<Resource> resources = incoming.getResourceStack();
        while (!resources.isEmpty()) {
            Resource r = resources.pop();
            Resource peer = this.getResourceWithId(r.getResourceId());
            if (peer == null) {
                // no corresponding resource in this table; use incoming
                // XXX PLM: Why is this needed? Only ever called on global
                // table, which is thrown away and replaced by incoming table
                this.addResource(r, Resource.RESOURCE_STATUS_INSTALLED);
            } else {
                if (r.isNewer(peer)) {
                    // Mark as being ready to transition
                    this.commit(peer, Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE);

                    if (!peer.getInstaller().unstage(peer, Resource.RESOURCE_STATUS_UNSTAGED)) {
                        // TODO: revert this resource table!
                        throw new UnresolvedResourceException(peer,
                                "Couldn't make room for new resource " +
                                        r.getResourceId() + ", upgrade aborted");
                    } else {
                        // done
                        commit(peer, Resource.RESOURCE_STATUS_UNSTAGED);
                    }

                    if (r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE) {
                        incoming.commit(r, Resource.RESOURCE_STATUS_UPGRADE_TO_INSTALL);
                        if (r.getInstaller().upgrade(r)) {
                            incoming.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
                        } else {
                            Logger.log("Resource",
                                    "Failed to upgrade resource: " + r.getDescriptor());
                            // REVERT!
                            return false;
                        }
                    }
                }
                // TODO Should anything happen if peer.getVersion() ==
                // r.getVersion()?  Consider children, IDs and the fact
                // resource locations could change
            }
        }

        return true;
    }

    /**
     * Flag unstaged resources and those not present in replacement table for
     * deletion.
     *
     * @param replacement Resources not in this table, flag for deletion
     */
    public void flagForDeletions(ResourceTable replacement) {
        Stack<Resource> s = this.getResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();

            if (replacement.getResourceWithId(r.getResourceId()) == null) {
                // no entry in 'replacement' so it's no longer relevant
                this.commit(r, Resource.RESOURCE_STATUS_DELETE);
                continue;
            }

            if (r.getStatus() == Resource.RESOURCE_STATUS_UNSTAGED) {
                // resource has been replaced, so flag for deletion
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
        Stack<Resource> s =
                this.getResourceStackWithStatus(Resource.RESOURCE_STATUS_UNSTAGED);
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
        Stack<Resource> s = this.getResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.getStatus() == Resource.RESOURCE_STATUS_DELETE) {
                try {
                    r.getInstaller().uninstall(r);
                } catch (Exception e) {
                    Logger.log("Resource", "Error uninstalling resource " +
                            r.getRecordGuid() + ". " + e.getMessage());
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
        for (Resource r : this.getResources()) {
            r.setID(-1);
            newTable.commit(r);
        }
    }

    /**
     * String representation of the id, version, and status of all resources in
     * table.
     */
    public String toString() {
        StringBuffer resourceDetails = new StringBuffer();
        int maxLength = 0;
        for (Resource r : getResources()) {
            String line = "| " + r.getResourceId() + " | " + r.getVersion() +
                    " | " + getStatusString(r.getStatus()) + " |\n";
            resourceDetails.append(line);

            if (line.length() > maxLength) {
                maxLength = line.length();
            }
        }

        StringBuffer header = new StringBuffer();
        for (int i = 0; i < maxLength; ++i) {
            header.append("-");
        }

        header.append("\n");

        return header.append(resourceDetails.toString()).append(header.toString()).toString();
    }

    public static String getStatusString(int status) {
        switch (status) {
            case Resource.RESOURCE_STATUS_UNINITIALIZED:
                return "Uninitialized";
            case Resource.RESOURCE_STATUS_LOCAL:
                return "Local";
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
     * Destroy this table, but leave any of the files which are installed
     * untouched. This is useful after an upgrade if this is the temp table.
     */
    public void destroy() {
        cleanup();
        storage.removeAll();
    }

    /**
     * Destroy this table, and also try very hard to remove any files installed
     * by it. This is important for rolling back botched upgrades without
     * leaving their files around.
     */
    public void clear() {
        cleanup();
        Stack<Resource> s = this.getResourceStack();
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
        for (Resource r : getResources()) {
            r.getInstaller().cleanup();
        }
    }

    /**
     * Register the available resources in this table with the provided
     * CommCare instance.
     *
     * @param instance
     * @throws ResourceInitializationException
     */
    public void initializeResources(CommCareInstance instance)
            throws ResourceInitializationException {
        // HHaaaacckkk. (Some properties cannot be handled until after others
        // TODO: Replace this with some sort of sorted priority queue.
        Vector<ResourceInstaller> lateInit = new Vector<ResourceInstaller>();

        for (Resource r : this.getResources()) {
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
     * Gather derived references for the resource's local locations. Relative
     * location references that have a parent are contextualized before being
     * added.
     *
     * @param r resource for which local location references are being gathered
     * @param t table to look-up the resource's parents in
     * @return all local references a resource's potential locations
     */
    private static Vector<Reference> gatherResourcesLocalRefs(Resource r,
                                                              ResourceTable t) {
        Vector<Reference> ret = new Vector<Reference>();

        for (ResourceLocation location : r.getLocations()) {
            if (location.isRelative()) {
                if (r.hasParent()) {
                    Resource parent = t.getResourceWithGuid(r.getParentId());
                    if (parent != null) {
                        // Get local references for the parent resource's
                        // locations
                        Vector<Reference> parentRefs =
                                gatherResourcesLocalRefs(parent, t);
                        for (Reference context : parentRefs) {
                            addDerivedLocation(location, context, ret);
                        }
                    }
                }
            } else if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
                addDerivedLocation(location, null, ret);
            }
        }
        return ret;
    }

    /**
     * Gather derived references for a particular (relative) location
     * corresponding to the given resource.  If the  parent isn't found in the
     * current resource table, then look in the master table.
     *
     * @param location Specific location for the given resource
     * @param r        Resource for which local location references are being
     *                 gathered
     * @param t        Table to look-up the resource's parents in
     * @param m        Backup table to look-up the resource's parents in
     * @return All possible (derived) references pointing to a given locations
     */
    private static Vector<Reference> gatherLocationsRefs(ResourceLocation location,
                                                         Resource r,
                                                         ResourceTable t,
                                                         ResourceTable m) {
        Vector<Reference> ret = new Vector<Reference>();

        if (r.hasParent()) {
            Resource parent = t.getResourceWithGuid(r.getParentId());

            // If the local table doesn't have the parent ref, try the master
            if (parent == null && m != null) {
                parent = m.getResourceWithGuid(r.getParentId());
            }

            if (parent != null) {
                // loop over all local references for the parent
                Vector<Reference> parentRefs =
                        explodeAllReferences(location.getAuthority(), parent, t, m);
                for (Reference context : parentRefs) {
                    addDerivedLocation(location, context, ret);
                }
            }
        }
        return ret;
    }

    /**
     * Gather derived references for the resource's locations of a given type.
     * Relative location references that have a parent are contextualized
     * before being added. If a parent isn't found in the current resource
     * table, then look in the master table.
     *
     * @param type process locations with authorities of this type
     * @param r    resource for which local location references are being gathered
     * @param t    table to look-up the resource's parents in
     * @param m    backup table to look-up the resource's parents in
     * @return all possible (derived) references pointing to a resource's
     * locations
     */
    private static Vector<Reference> explodeAllReferences(int type,
                                                          Resource r,
                                                          ResourceTable t,
                                                          ResourceTable m) {
        Vector<Reference> ret = new Vector<Reference>();

        for (ResourceLocation location : r.getLocations()) {
            if (location.getAuthority() == type) {
                if (location.isRelative()) {
                    if (r.hasParent()) {
                        Resource parent = t.getResourceWithGuid(r.getParentId());

                        // If the local table doesn't have the parent ref, try
                        // the master
                        if (parent == null) {
                            parent = m.getResourceWithGuid(r.getParentId());
                        }
                        if (parent != null) {
                            // Get all local references for the parent
                            Vector<Reference> parentRefs =
                                    explodeAllReferences(type, parent, t, m);
                            for (Reference context : parentRefs) {
                                addDerivedLocation(location, context, ret);
                            }
                        }
                    }
                } else {
                    addDerivedLocation(location, null, ret);
                }
            }
        }
        return ret;
    }

    /**
     * Derive a reference from the given location and context; adding it to the
     * vector of references.
     *
     * @param location Contains a reference to a resource.
     * @param context  Provides context for any relative reference accessors.
     *                 Can be null.
     * @param ret      Add derived reference of location to this Vector.
     */
    private static void addDerivedLocation(ResourceLocation location,
                                           Reference context,
                                           Vector<Reference> ret) {
        try {
            final Reference derivedRef;
            if (context == null) {
                derivedRef =
                        ReferenceManager._().DeriveReference(location.getLocation());
            } else {
                // contextualize the location ref in terms of the multiple refs
                // pointing to different locations for the parent resource
                derivedRef =
                        ReferenceManager._().DeriveReference(location.getLocation(),
                                context);
            }
            ret.addElement(derivedRef);
        } catch (InvalidReferenceException e) {
            e.printStackTrace();
        }
    }


    public void verifyInstallation(Vector<MissingMediaException> problems) {
        Vector<Resource> resources = getResources();
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

    public void setStateListener(TableStateListener listener) {
        this.stateListener = listener;
    }

    public void setInstallCancellationChecker(InstallCancelled cancellationChecker) {
        this.cancellationChecker = cancellationChecker;
    }

    public void setInstallStatsLogger(InstallStatsLogger logger) {
        this.installStatsLogger = logger;
    }

    /**
     * Sets the number of attempts this table will make to install against
     * resources which fail on lossy (IE: Network) channels.
     *
     * @param number The number of attempts to make per resource. Must be at
     *               least 0
     */
    public void setNumberOfRetries(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Can't have less than 0 retries");
        }
        this.numberOfLossyRetries = number;
    }
}

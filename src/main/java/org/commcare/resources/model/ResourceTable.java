package org.commcare.resources.model;

import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.util.Enumeration;
import java.util.Hashtable;
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

    private static final int NUMBER_OF_LOSSY_RETRIES = 3;
    // Tracks whether a compound resource has been added, requiring
    // recalculation of how many uninstalled resources there are.  Where
    // 'compound resources' are those that contain references to more
    // resources, such as profile and suite resources.
    private boolean isResourceProgressStale = false;
    // Cache for profile and suite 'parent' resources which are used in
    // references resolution
    private final Hashtable<String, Resource> compoundResourceCache =
            new Hashtable<>();

    public ResourceTable() {
    }

    protected ResourceTable(IStorageUtilityIndexed storage,
                            InstallerFactory factory) {
        this.storage = storage;
        this.factory = factory;
    }

    public boolean isEmpty() {
        return storage.getNumRecords() <= 0;
    }

    public static ResourceTable RetrieveTable(IStorageUtilityIndexed storage) {
        return RetrieveTable(storage, new InstallerFactory());
    }

    public static ResourceTable RetrieveTable(IStorageUtilityIndexed storage,
                                              InstallerFactory factory) {
        return new ResourceTable(storage, factory);
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
        } 
    }

    protected boolean resourceDoesntExist(Resource resource) {
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

        commit(resource);
    }

    public Vector<Resource> getResourcesForParent(String parent) {
        Vector<Resource> v = new Vector<>();
        for (Enumeration en = storage.getIDsForValue(Resource.META_INDEX_PARENT_GUID, parent).elements(); en.hasMoreElements(); ) {
            Resource r = (Resource)storage.read((Integer)en.nextElement());
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

    private Resource getParentResource(Resource resource) {
        String parentId = resource.getParentId();
        if (parentId != null && !"".equals(parentId)) {
            if (compoundResourceCache.containsKey(parentId)) {
                return compoundResourceCache.get(parentId);
            } else {
                try {
                    Resource parent =
                            (Resource)storage.getRecordForValue(Resource.META_INDEX_RESOURCE_GUID, parentId);
                    compoundResourceCache.put(parentId, parent);
                    return parent;
                } catch (NoSuchElementException nsee) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get the all the resources in this table's storage.
     */
    private Vector<Resource> getResources() {
        Vector<Resource> v = new Vector<>();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            v.addElement(r);
        }
        return v;
    }

    /**
     * Get the all the resources in this table's storage.
     */
    private Stack<Resource> getResourceStack() {
        Stack<Resource> v = new Stack<>();
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
        Stack<Resource> v = new Stack<>();
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
        Stack<Resource> v = new Stack<>();
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
        return getUnreadyResources().isEmpty();
    }

    public void commitCompoundResource(Resource r, int status, int version)
            throws UnresolvedResourceException {
        if (r.getVersion() == Resource.RESOURCE_VERSION_UNKNOWN) {
            // Try to update the version.
            r.setVersion(version);
        } else {
            // Otherwise, someone screwed up
            Logger.log("Resource", "committing a resource with a known version.");
        }
        commitCompoundResource(r, status);
    }

    /**
     * Add a 'compound' resource, which has references to other resources.
     *
     * @param r profile, suite, media suite, or other 'compound' resource
     */
    public void commitCompoundResource(Resource r, int status) {
        compoundResourceCache.put(r.getResourceId(), r);
        isResourceProgressStale = true;
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
    public void rollbackCommits(CommCarePlatform platform) {
        Stack<Resource> s = this.getResourceStack();
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.isDirty()) {
                this.commit(r, r.getInstaller().rollback(r, platform));
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
     * @param platform The CommCare platform (specific profile and version) to
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
                                                CommCarePlatform platform,
                                                ResourceTable master)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {

        // TODO: Possibly check if resource status is local and proceeding to
        // skip this huge (although in reality like one step) chunk

        UnreliableSourceException unreliableSourceException = null;
        InvalidResourceException invalidResourceException = null;

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
                                    platform, upgrade);
                        } catch (InvalidResourceException e) {
                            invalidResourceException = e;
                        } catch (UnreliableSourceException use) {
                            unreliableSourceException = use;
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
                            ReferenceManager.instance().DeriveReference(location.getLocation()),
                            this, platform, upgrade);
                    if (handled) {
                        recordSuccess(r);
                        break;
                    }
                } catch (InvalidResourceException e) {
                    invalidResourceException = e;
                } catch (InvalidReferenceException ire) {
                    ire.printStackTrace();
                    // Continue until no resources can be found.
                } catch (UnreliableSourceException use) {
                    unreliableSourceException = use;
                }
            }
        }

        if (!handled) {
            if (invalidResourceException != null) {
                throw invalidResourceException;
            } else if (unreliableSourceException == null) {
                // no particular failure to point our finger at.
                throw new UnresolvedResourceException(r,
                        "No external or local definition could be found for resource " +
                                r.getResourceId());
            } else {
                // Expose the lossy failure rather than the generic one
                throw unreliableSourceException;
            }
        }
    }

    /**
     * Makes all of this table's resources available.
     *
     * @param master   The global resource to prepare against. Used to
     *                 establish whether resources need to be fetched remotely
     * @param platform The platform (version and profile) to prepare against
     * @throws UnresolvedResourceException       If a resource could not be
     *                                           identified and is required
     * @throws UnfullfilledRequirementsException If some resources are
     *                                           incompatible with the current
     *                                           version of CommCare
     */
    public void prepareResources(ResourceTable master, CommCarePlatform platform)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {

        Hashtable<String, Resource> masterResourceMap = null;
        if (master != null) {
            // avoid hitting storage in loops by front-loading resource
            // acquisition from master table
            masterResourceMap = getResourceMap(master);
        }
        Vector<Resource> unreadyResources = getUnreadyResources();

        // install all unready resources.
        while (!unreadyResources.isEmpty()) {
            for (Resource r : unreadyResources) {
                prepareResource(master, platform, r, masterResourceMap);
            }
            // Installing resources may have exposed more unready resources
            // that need installing.
            unreadyResources = getUnreadyResources();
        }
    }

    private static Hashtable<String, Resource> getResourceMap(ResourceTable table) {
        Hashtable<String, Resource> resourceMap = new Hashtable<>();
        for (IStorageIterator it = table.storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            resourceMap.put(r.getResourceId(), r);
        }
        return resourceMap;
    }

    /**
     * Makes all resources available until toInitialize is encountered.
     *
     * @param master       The global resource to prepare against. Used to
     *                     establish whether resources need to be fetched remotely
     * @param platform     The platform (version and profile) to prepare against
     * @param toInitialize The ID of a single resource after which the table
     *                     preparation can stop.
     * @throws UnresolvedResourceException       Required resource couldn't be
     *                                           identified
     * @throws UnfullfilledRequirementsException resource(s) incompatible with
     *                                           current CommCare version
     */
    public void prepareResourcesUpTo(ResourceTable master,
                                     CommCarePlatform platform,
                                     String toInitialize)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {

        Vector<Resource> unreadyResources = getUnreadyResources();

        // install unready resources, until toInitialize has been installed.
        while (isResourceUninitialized(toInitialize) && !unreadyResources.isEmpty()) {
            for (Resource r : unreadyResources) {
                prepareResource(master, platform, r, null);
            }
            // Installing resources may have exposed more unready resources
            // that need installing.
            unreadyResources = getUnreadyResources();
        }
    }

    /**
     * @param master            The global resource to prepare against. Used to
     *                          establish whether resources need to be fetched
     *                          remotely
     * @param masterResourceMap Map from resource id to resources for master
     *                          table. Null when 'master' is, or when
     *                          pre-loading the resource map isn't worth it.
     */
    private void prepareResource(ResourceTable master, CommCarePlatform platform,
                                 Resource r, Hashtable<String, Resource> masterResourceMap)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {
        boolean upgrade = false;

        Vector<Reference> invalid = new Vector<>();

        if (master != null) {
            Resource peer;
            // obtain resource peer by looking up the current resource
            // in the master table
            if (masterResourceMap == null) {
                peer = master.getResourceWithId(r.getResourceId());
            } else {
                peer = masterResourceMap.get(r.getResourceId());
            }
            if (peer != null) {
                if (!r.isNewer(peer)) {
                    // This resource doesn't need to be updated, copy
                    // the existing resource into this table
                    peer.mimick(r);
                    commit(peer, Resource.RESOURCE_STATUS_INSTALLED);

                    if (stateListener != null) {
                        // copying a resource over shouldn't add anymore
                        // resources to be processed
                        stateListener.simpleResourceAdded();
                    }
                    return;
                }

                // resource is newer than master version, so invalidate
                // old local resource locations.
                upgrade = true;
                invalid = ResourceTable.gatherResourcesLocalRefs(peer, master);
            }
        }

        findResourceLocationAndInstall(r, invalid, upgrade, platform, master);

        if (stateListener != null) {
            if (isResourceProgressStale) {
                // a compound resource was added, recalculate total resource count
                isResourceProgressStale = false;
                stateListener.compoundResourceAdded(this);
            } else {
                stateListener.simpleResourceAdded();
            }
        }
    }

    /**
     * Force a recomputation of table stage progress; useful for resuming upgrades
     */
    public void setResourceProgressStale() {
        isResourceProgressStale = true;
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
                                    CommCarePlatform platform, boolean upgrade)
            throws UnresolvedResourceException, UnfullfilledRequirementsException, InstallCancelledException {
        UnreliableSourceException aFailure = null;

        for (int i = 0; i < NUMBER_OF_LOSSY_RETRIES + 1; ++i) {
            abortIfInstallCancelled(r);
            try {
                return r.getInstaller().install(r, location, ref, table, platform, upgrade);
            } catch (UnreliableSourceException use) {
                recordFailure(r, use);
                aFailure = use;
                Logger.log("install", "Potentially lossy install attempt # " +
                        (i + 1) + " of " + (NUMBER_OF_LOSSY_RETRIES + 1) +
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
     */
    public void upgradeTable(ResourceTable incoming, CommCarePlatform platform) throws UnresolvedResourceException {
        if (!incoming.isReady()) {
            throw new RuntimeException("Incoming table is not ready to be upgraded");
        }

        // Everything incoming should be marked either ready or upgrade.
        // Upgrade elements should result in their counterpart in this table
        // being unstaged (which can be reverted).
        Hashtable<String, Resource> resourceMap = getResourceMap(this);
        for (IStorageIterator it = incoming.storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            Resource peer = resourceMap.get(r.getResourceId());
            if (peer == null) {
                // no corresponding resource in this table; use incoming
                addResource(r, Resource.RESOURCE_STATUS_INSTALLED);
            } else {
                if (r.isNewer(peer)) {
                    // Mark as being ready to transition
                    commit(peer, Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE);

                    if (!peer.getInstaller().unstage(peer, Resource.RESOURCE_STATUS_UNSTAGED, platform)) {
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
                        if (r.getInstaller().upgrade(r, platform)) {
                            incoming.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
                        } else {
                            Logger.log("Resource",
                                    "Failed to upgrade resource: " + r.getDescriptor());
                            // REVERT!
                            throw new RuntimeException("Failed to upgrade resource " + r.getDescriptor());
                        }
                    }
                }
                // TODO Should anything happen if peer.getVersion() ==
                // r.getVersion()?  Consider children, IDs and the fact
                // resource locations could change
            }
        }
    }

    /**
     * Uninstall table by removing unstaged resources and those not present in
     * replacement table
     *
     * This method is the final step in an update, after this table has
     * already been moved to a placeholder table and been evaluated for
     * what resources are no longer necessary.
     *
     * If this table encounters any problems it will not intentionally
     * throw errors, assuming that it's preferable to leave data unremoved
     * rather than breaking the app.
     *
     * @param replacement Reference table; uninstall resources not also present
     *                    in this table
     */
    public void uninstall(ResourceTable replacement, CommCarePlatform platform) {
        cleanup();
        Hashtable<String, Resource> replacementMap = getResourceMap(replacement);
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            if (replacementMap.get(r.getResourceId()) == null ||
                    r.getStatus() == Resource.RESOURCE_STATUS_UNSTAGED) {
                // No entry in 'replacement' so it's no longer relevant
                // OR resource has been replaced, so flag for deletion
                try {
                    r.getInstaller().uninstall(r, platform);
                } catch (Exception e) {
                    Logger.log("Resource", "Error uninstalling resource " +
                            r.getRecordGuid() + ". " + e.getMessage());
                }
            } else if (r.getStatus() == Resource.RESOURCE_STATUS_DELETE) {
                // NOTE: Shouldn't be a way for this condition to occur, but check anyways...
                try {
                    r.getInstaller().uninstall(r, platform);
                } catch (Exception e) {
                    Logger.log("Resource", "Error uninstalling resource " +
                            r.getRecordGuid() + ". " + e.getMessage());
                }
            }
        }

        storage.removeAll();
    }


    /**
     * Called on a table to restage any unstaged resources.
     *
     * @param incoming The table which unstaged this table's resources
     */
    public void repairTable(ResourceTable incoming, CommCarePlatform platform) {
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
                    if (!peer.getInstaller().unstage(peer, Resource.RESOURCE_STATUS_UPGRADE, platform)) {
                        // TODO: IF there are errors here, signal that the incoming table
                        // should just be wiped out. It's not in acceptable shape
                    } else {
                        incoming.commit(peer, Resource.RESOURCE_STATUS_UPGRADE);
                    }
                }
            }

            // Way should be clear.
            this.commit(resource, Resource.RESOURCE_STATUS_UNSTAGE_TO_INSTALL);
            if (resource.getInstaller().revert(resource, this, platform)) {
                this.commit(resource, Resource.RESOURCE_STATUS_INSTALLED);
            }
        }
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
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
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
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
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
    public void clear(CommCarePlatform platform) {
        cleanup();
        Stack<Resource> s = this.getResourceStack();
        int count = 0;
        while (!s.isEmpty()) {
            Resource r = s.pop();
            if (r.getStatus() == Resource.RESOURCE_STATUS_UPGRADE) {
                try {
                    r.getInstaller().uninstall(r, platform);
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

    protected void cleanup() {
        compoundResourceCache.clear();
        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            r.getInstaller().cleanup();
        }
    }

    /**
     * Register the available resources in this table with the provided
     * CommCare platform.
     */
    public void initializeResources(CommCarePlatform platform, boolean isUpgrade) {
        // HHaaaacckkk. (Some properties cannot be handled until after others
        // TODO: Replace this with some sort of sorted priority queue.
        Vector<ResourceInstaller> lateInit = new Vector<>();

        for (IStorageIterator it = storage.iterate(); it.hasMore(); ) {
            Resource r = (Resource)it.nextRecord();
            ResourceInstaller i = r.getInstaller();
            if (i.requiresRuntimeInitialization()) {
                if (i instanceof ProfileInstaller) {
                    lateInit.addElement(i);
                } else {
                    i.initialize(platform, isUpgrade);
                }
            }
        }
        for (ResourceInstaller i : lateInit) {
            i.initialize(platform, isUpgrade);
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
        Vector<Reference> ret = new Vector<>();

        for (ResourceLocation location : r.getLocations()) {
            if (location.isRelative()) {
                if (r.hasParent()) {
                    Resource parent = t.getParentResource(r);
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
        Vector<Reference> ret = new Vector<>();

        if (r.hasParent()) {
            Resource parent = t.getParentResource(r);

            // If the local table doesn't have the parent ref, try the master
            if (parent == null && m != null) {
                parent = m.getParentResource(r);
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
        Vector<Reference> ret = new Vector<>();

        for (ResourceLocation location : r.getLocations()) {
            if (location.getAuthority() == type) {
                if (location.isRelative()) {
                    if (r.hasParent()) {
                        Resource parent = t.getParentResource(r);

                        // If the local table doesn't have the parent ref, try
                        // the master
                        if (parent == null) {
                            parent = m.getParentResource(r);
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
                        ReferenceManager.instance().DeriveReference(location.getLocation());
            } else {
                // contextualize the location ref in terms of the multiple refs
                // pointing to different locations for the parent resource
                derivedRef =
                        ReferenceManager.instance().DeriveReference(location.getLocation(),
                                context);
            }
            ret.addElement(derivedRef);
        } catch (InvalidReferenceException e) {
            e.printStackTrace();
        }
    }


    public void verifyInstallation(Vector<MissingMediaException> problems, CommCarePlatform platform) {
        Vector<Resource> resources = getResources();
        int total = resources.size();
        int count = 0;
        for (Resource r : resources) {
            r.getInstaller().verifyInstallation(r, problems, platform);
            count++;
            if (stateListener != null) {
                stateListener.incrementProgress(count, total);
            }
            if (cancellationChecker != null && cancellationChecker.wasInstallCancelled()) {
                break;
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
}

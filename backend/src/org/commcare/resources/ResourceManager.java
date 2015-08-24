package org.commcare.resources;

import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.InstallCancelled;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.TableStateListener;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.util.Vector;

/**
 * Resource table install and update logic.
 */
public class ResourceManager {
    protected final CommCarePlatform platform;
    protected final ResourceTable masterTable;
    protected final ResourceTable upgradeTable;
    protected final ResourceTable tempTable;

    public ResourceManager(CommCarePlatform platform,
                           ResourceTable masterTable,
                           ResourceTable upgradeTable,
                           ResourceTable tempTable) {
        this.platform = platform;
        this.masterTable = masterTable;
        this.upgradeTable = upgradeTable;
        this.tempTable = tempTable;
    }

    /**
     * Installs resources described by profile reference into the provided
     * resource table. If the resource table is ready or already has a profile,
     * don't do anything.
     *
     * @param profileReference URL to profile file
     * @param global           Add profile ref to this table and install its
     *                         resources
     * @param forceInstall     Should installation be performed regardless of
     *                         version numbers?
     */
    public static void installAppResources(CommCarePlatform platform, String profileReference,
                                           ResourceTable global, boolean forceInstall)
            throws UnfullfilledRequirementsException,
            UnresolvedResourceException,
            InstallCancelledException {
        try {
            if (!global.isReady()) {
                global.prepareResources(null, platform);
            }

            // First, see if the appropriate profile exists
            Resource profile =
                    global.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);

            if (profile == null) {
                // grab the local profile and parse it
                Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
                locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, profileReference));

                // We need a way to identify this version...
                Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN,
                        CommCarePlatform.APP_PROFILE_RESOURCE_ID,
                        locations, "Application Descriptor");

                global.addResource(r, global.getInstallers().getProfileInstaller(forceInstall), "");
                global.prepareResources(null, platform);
            }
        } catch (StorageFullException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the app's authoritative profile into the upgrade table.
     *
     * @param clearProgress Clear the 'incoming' table of any partial update
     *                      info.
     */
    public void stageUpgradeTable(boolean clearProgress)
            throws UnfullfilledRequirementsException,
            StorageFullException,
            UnresolvedResourceException, InstallCancelledException {
        Profile current = platform.getCurrentProfile();
        String profileRef = current.getAuthReference();
        stageUpgradeTable(profileRef, clearProgress);
    }

    /**
     * Loads the profile at the provided reference into the upgrade table.
     *
     * @param clearProgress Clear the 'incoming' table of any partial update
     *                      info.
     */
    public void stageUpgradeTable(String profileRef, boolean clearProgress)
            throws UnfullfilledRequirementsException,
            StorageFullException,
            UnresolvedResourceException, InstallCancelledException {

        ensureValidState();

        if (clearProgress) {
            upgradeTable.clear();
        }

        loadProfile(upgradeTable, profileRef);
    }

    protected void ensureValidState() {
        if (masterTable.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
            repair();

            if (masterTable.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
                throw new IllegalArgumentException("Global resource table was not ready for upgrading");
            }
        }
    }

    protected void loadProfile(ResourceTable incoming,
                               String profileRef)
            throws UnfullfilledRequirementsException,
            UnresolvedResourceException,
            InstallCancelledException {
        Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
        locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_REMOTE, profileRef));

        Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN,
                CommCarePlatform.APP_PROFILE_RESOURCE_ID, locations,
                "Application Descriptor");

        incoming.addResource(r,
                incoming.getInstallers().getProfileInstaller(false),
                null);

        incoming.prepareResourcesUpTo(masterTable,
                this.platform,
                CommCarePlatform.APP_PROFILE_RESOURCE_ID);
    }

    /**
     * Download resources referenced by upgrade table's profile into the
     * upgrade table itself.
     *
     * @throws InstallCancelledException The user/system has cancelled the
     *                                   installation process
     */
    public void prepareUpgradeResources()
            throws UnfullfilledRequirementsException,
            UnresolvedResourceException, IllegalArgumentException,
            InstallCancelledException {
        ensureValidState();

        // TODO: Figure out more cleanly what the acceptable states are here
        int upgradeTableState = upgradeTable.getTableReadiness();
        if (upgradeTableState == ResourceTable.RESOURCE_TABLE_UNCOMMITED ||
                upgradeTableState == ResourceTable.RESOURCE_TABLE_UNSTAGED ||
                upgradeTableState == ResourceTable.RESOURCE_TABLE_EMPTY) {
            throw new IllegalArgumentException("Upgrade table is not in an appropriate state");
        }

        // Wipe out any existing records in the tempTable table. If there's
        // _anything_ in there and the app isn't in the install state, that's a
        // signal to recover.
        tempTable.destroy();

        // Fetch and prepare all resources (Likely exit point here if a
        // resource can't be found)
        upgradeTable.prepareResources(masterTable, this.platform);
    }

    /**
     * Install staged upgrade table into the global table.
     */
    public void upgrade()
            throws UnresolvedResourceException, IllegalArgumentException {
        boolean upgradeSuccess = false;
        try {
            Logger.log("Resource", "Upgrade table fetched, beginning upgrade");
            //Try to stage the upgrade table to replace the incoming table
            if (!masterTable.upgradeTable(upgradeTable)) {
                throw new RuntimeException("global table failed to upgrade!");
            } else if (upgradeTable.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
                throw new RuntimeException("not all incoming resources were installed!!");
            } else {
                //otherwise keep going
                Logger.log("Resource", "Global table unstaged, upgrade table ready");
            }

            // Now we basically want to replace the global resource table with
            // the upgrade table

            // ok, so temporary should now be fully installed.  make a copy of
            // our table just in case.

            Logger.log("Resource", "Copying global resources to recovery area");
            try {
                masterTable.copyToTable(tempTable);
            } catch (RuntimeException e) {
                // The _only_ time the recovery table should have data is if we
                // were in the middle of an install. Since global hasn't been
                // modified if there is a problem here we want to wipe out the
                // recovery stub

                //TODO: If this fails? Oof.
                tempTable.destroy();
                throw e;
            }

            Logger.log("Resource", "Wiping global");
            //now clear the global table to make room (but not the data, just the records)
            masterTable.destroy();

            Logger.log("Resource", "Moving update resources");
            //Now copy the upgrade table to take its place
            upgradeTable.copyToTable(masterTable);

            //Success! The global table should be ready to go now.
            upgradeSuccess = true;

            Logger.log("Resource", "Upgrade Succesful!");

            //Now we need to do cleanup. Wipe out the upgrade table and finalize
            //removing the original resources which are no longer needed.

            //Wipe the incoming (we need to do nothing with its resources)
            Logger.log("Resource", "Wiping redundant update table");
            upgradeTable.destroy();

            //Uninstall old resources
            Logger.log("Resource", "Clearing out old resources");
            tempTable.flagForDeletions(masterTable);
            tempTable.completeUninstall();

            //good to go.
        } finally {
            if (!upgradeSuccess) {
                repair();
            }
            // TODO PLM: how necessary is this?
            platform.clearAppState();

            //Is it really possible to verify that we've un-registered
            //everything here? Locale files are registered elsewhere, and we
            //can't guarantee we're the only thing in there, so we can't
            //straight up clear it...
            platform.initialize(masterTable);
        }
    }

    /**
     * This method is responsible for recovering the state of the application
     * to installed after anything happens during an upgrade. After it is
     * finished, the global resource table should be valid.
     *
     * NOTE: this does not currently repair resources which have been
     * corrupted, merely returns all of the tables to the appropriate states
     */
    private void repair() {
        // First we need to figure out what state we're in currently. There are
        // a few possibilities

        // TODO: Handle: Upgrade complete (upgrade table empty, all resources
        // pushed to global), recovery table not empty

        // First possibility is needing to restore from the recovery table.
        if (!tempTable.isEmpty()) {
            // If the recovery table isn't empty, we're likely restoring from
            // there. We need to check first whether the global table has the
            // same profile, or the recovery table simply doesn't have one in
            // which case the recovery table didn't get copied correctly.
            Resource tempProfile =
                    tempTable.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
            Resource masterProfile =
                    masterTable.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
            if (tempProfile == null ||
                    (masterProfile.getVersion() == tempProfile.getVersion())) {
                Logger.log("resource", "Invalid recovery table detected. Wiping recovery table");
                // This means the recovery table should be empty. Invalid copy.
                tempTable.destroy();
            } else {
                // We need to recover the global resources from the recovery
                // table.
                Logger.log("resource", "Recovering global resources from recovery table");

                masterTable.destroy();
                tempTable.copyToTable(masterTable);

                Logger.log("resource", "Global resources recovered. Wiping recovery table");
                tempTable.destroy();
            }
        }

        // Global and incoming are now in the right places. Ensure we have no
        // uncommitted resources.
        if (masterTable.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNCOMMITED) {
            masterTable.rollbackCommits();
        }

        if (upgradeTable.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNCOMMITED) {
            upgradeTable.rollbackCommits();
        }

        // If the global table needed to be recovered from the recovery table,
        // it has. There are now two states: Either the global table is fully
        // installed (no conflicts with the upgrade table) or it has unstaged
        // resources to restage
        if (masterTable.getTableReadiness() == ResourceTable.RESOURCE_TABLE_INSTALLED) {
            Logger.log("resource", "Global table in fully installed mode. Repair complete");
        } else if (masterTable.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNSTAGED) {
            Logger.log("resource", "Global table needs to restage some resources");
            masterTable.repairTable(upgradeTable);
        }
    }

    /**
     * Set listeners and checkers that enable communication between low-level
     * resource installation and top-level app update/installation process.
     *
     * @param tableListener  allows resource table to report its progress to the
     *                       launching process
     * @param cancelCheckker allows resource installers to check if the
     *                       launching process was cancelled
     */
    public void setUpgradeListeners(TableStateListener tableListener,
                                    InstallCancelled cancelCheckker) {
        masterTable.setStateListener(tableListener);
        upgradeTable.setStateListener(tableListener);

        upgradeTable.setInstallCancellationChecker(cancelCheckker);
    }

    /**
     * @return Is the table non-empty, marked for upgrade, with all ready
     * resources?
     */
    public static boolean isTableStagedForUpgrade(ResourceTable table) {
        return (table.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UPGRADE &&
                table.isReady() &&
                !table.isEmpty());
    }

    public boolean isUpgradeTableStaged() {
        return isTableStagedForUpgrade(upgradeTable);
    }

    public boolean updateIsntNewer(Resource currentProfile) {
        Resource newProfile =
                upgradeTable.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
        return newProfile != null && !newProfile.isNewer(currentProfile);
    }

    public Resource getMasterProfile() {
        return masterTable.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
    }

    public static Vector<Resource> getResourceListFromProfile(ResourceTable master) {
        Vector<Resource> unresolved = new Vector<Resource>();
        Vector<Resource> resolved = new Vector<Resource>();
        Resource r = master.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
        if (r == null) {
            return resolved;
        }
        unresolved.addElement(r);
        while (unresolved.size() > 0) {
            Resource current = unresolved.firstElement();
            unresolved.removeElement(current);
            resolved.addElement(current);
            Vector<Resource> children = master.getResourcesForParent(current.getRecordGuid());
            for (Resource child : children) {
                unresolved.addElement(child);
            }
        }
        return resolved;
    }
}

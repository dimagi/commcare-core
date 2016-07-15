package org.commcare.resources.model;

import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.util.Vector;

/**
 * <p>A Resource Installer (Possible name change pending) is
 * responsible for taking a Resource definition and taking
 * steps to make it available in the local environment.</p>
 *
 * <p>Resource record objects record and keep track of the state
 * of resources, while installers actually manage the local
 * representation of the resource and manage their initialization
 * and how to get them installed or uninstalled</p>
 *
 * <p>Currently these installers are tracked as part of the resource
 * record itself. However, this will probably change as they are
 * transitioned to a device-specific factory. </p>
 *
 * @author ctsims
 */
public interface ResourceInstaller<T extends CommCareInstance> extends Externalizable {

    /**
     * @return true if a resource requires an initialization at
     * runtime in order to work properly. False otherwise.
     * This method may be unnecessary.
     */
    boolean requiresRuntimeInitialization();

    /**
     * initializes an installed resource for use at runtime.
     *
     * @param isUpgrade true when initializion happening
     * @return true if a resource is ready for use. False if
     * a problem occurred.
     * @throws ResourceInitializationException If the resource could not be initialized
     */
    boolean initialize(T instance, boolean isUpgrade) throws ResourceInitializationException;

    /**
     * Proceeds with the next step of installing resource r, keeping records at
     * current, and maintaining upgrade status against master.
     *
     * @param r        The resource to be stepped
     * @param table    the table where the resource is being managed
     * @return Whether the resource was able to complete an installation
     * step in the current circumstances.
     * @throws UnresolvedResourceException       If the local resource
     *                                           definition could not be found
     * @throws UnfullfilledRequirementsException If the current platform does
     *                                           not fullfill the needs for this resource
     */
    boolean install(Resource r, ResourceLocation location,
                    Reference ref, ResourceTable table,
                    T instance, boolean upgrade) throws
            UnresolvedResourceException, UnfullfilledRequirementsException;

    /**
     * Removes the binary files and cached data associated with a resource, often in order to
     * overwrite their old location with a new resource.
     *
     * This method _should only_ be called on a resource table that will never be made ready again.
     */
    boolean uninstall(Resource r) throws UnresolvedResourceException;

    /**
     * Called on a resource which is fully installed in the current environment and will be replaced by an incoming
     * resource from an upgrade table.
     *
     * This method must be reversible by calling the "revert" method, so no files should be deleted or permanently removed.
     *
     * After being unstaged, a resource's status will be set by the resource table.
     */
    boolean unstage(Resource r, int newStatus);

    /**
     * Revert is called on a resource in the unstaged state. It re-registers an existing resource in the current environment
     * after an unsuccesful upgrade or other issue.
     */
    boolean revert(Resource r, ResourceTable table);

    /**
     * Rolls back an incomplete action.
     *
     * @return the new status of this resource
     */
    int rollback(Resource r);

    /**
     * Upgrade is called when an incoming resource has had its conflicting peer unstaged.
     *
     * This method should result in the existing resource being marked as "installed" in the
     * existing table and the resource being ready for use.
     *
     * This method should be revertable.
     *
     * @param r The resource to be upgraded.
     * @return True if the upgrade step was completed successfully.
     * @throws UnresolvedResourceException If the local resource definition could not be found
     */
    boolean upgrade(Resource r) throws UnresolvedResourceException;

    /**
     * Called to clean up or close any interstitial state that was created by managing this resource.
     */
    void cleanup();

    boolean verifyInstallation(Resource r, Vector<MissingMediaException> problemList);
}

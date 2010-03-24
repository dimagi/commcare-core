/**
 * 
 */
package org.commcare.resources.model;

import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.util.externalizable.Externalizable;

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
 *
 */
public interface ResourceInstaller extends Externalizable {
	
	/**
	 * @return true if a resource requires an initialization at
	 * runtime in order to work properly. False otherwise.
	 * This method may be unnecessary.
	 */
	public boolean requiresRuntimeInitialization();
	
	/**
	 * initializes an installed resource for use at runtime.
	 * @return true if a resource is ready for use. False if
	 * a problem occurred.
	 * @throws ResourceInitializationException If the resource could not be initialized
	 */
	public boolean initialize(CommCareInstance instance) throws ResourceInitializationException;
	
	/**
	 * Proceeds with the next step of installing resource r, keeping
	 * records at current, and maintaining upgrade status against
	 * master.
	 * 
	 * @param r The resource to be stepped
	 * @param table the table where the resource is being managed
	 * @param peer the current copy of a resource (if one exists)
	 * @return Whether the resource was able to complete an installation
	 * step in the current circumstances.
	 * @throws UnresolvedResourceException If the local resource definition could not be found
	 */
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) throws UnresolvedResourceException;
	
	/**
	 * Removes the binary files and cached data associated with a resource, often in order to 
	 * overwrite their old location with a new resource.
	 */
	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) throws UnresolvedResourceException;
	
	/**
	 * Upgrade is called when a resource is installed locally, but is waiting for a 
	 * previous version of itself to be uninstalled. This method generally moves 
	 * any unique indexes from a temporary value to the appropriate value which would
	 * be used for the installed resource.
	 * 
	 * After this step is completed, the resource should be marked as installed. 
	 * 
	 * @param r The resource to be upgraded.
	 * @param table The table in which the resource belongs.
	 * @return True if the upgrade step was completed successfully.
	 * @throws UnresolvedResourceException If the local resource definition could not be found
	 */
	public boolean upgrade(Resource r, ResourceTable table) throws UnresolvedResourceException;
	
	public void cleanup();
}

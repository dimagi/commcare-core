/**
 * 
 */
package org.commcare.resources.model;

import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.util.externalizable.Externalizable;

/**
 * A resource installer is CommCare's local record of what a resource's
 * local status is. It is responsible for managing installation, uninstallation,
 * updates, initialization at runtime, etc.
 * 
 * While resource records manage a resource's abstract status (where it can
 * be found, what should be done with it), a resource installer is more accountable
 * for what to do with a resource while it's on the device.
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
	 * @return The next step to be completed after this one.
	 */
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) throws UnresolvedResourceException;
	
	/**
	 * Removes the binary files and cached data associated with a resource, often in order to 
	 * overwrite their old location with a new resource.
	 */
	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) throws UnresolvedResourceException;
	
	/**
	 * 
	 * @param r
	 * @param table
	 * @return
	 * @throws UnresolvedResourceException
	 */
	public boolean upgrade(Resource r, ResourceTable table) throws UnresolvedResourceException;
	
	public void cleanup();
}

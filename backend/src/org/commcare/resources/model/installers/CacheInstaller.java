/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * Used for any resources which:
 * 1) Are going to be stored in memory
 * 2) Possibly have derived resources
 * 3)
 * 
 *  NOTE: This functionality can probably be summed up into a 
 *  composite model, rather than an inheritance.
 * 
 * @author ctsims
 *
 */
public abstract class CacheInstaller implements ResourceInstaller<CommCareInstance> {

	private IStorageUtility cacheStorage;
	
	protected abstract String getCacheKey();
	
	protected IStorageUtility storage() {
		if(cacheStorage == null) {
			cacheStorage = StorageManager.getStorage(getCacheKey());
		}
		return cacheStorage;
	}
	
	int cacheLocation;
	
	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize(CommCareInstance instance) throws ResourceInitializationException {
		//Suites don't need any local initialization (yet).
		return false;
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
	 */
	public boolean requiresRuntimeInitialization() {
		//Nope.
		return false;
	}
	
	public abstract boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException;
	
	public boolean upgrade(Resource r, ResourceTable table) throws UnresolvedResourceException {
		//Don't need to do anything, since the resource is in the RMS already.
		throw new UnresolvedResourceException(r,"Attempt to upgrade installed resource suite");
	}

	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) throws UnresolvedResourceException {
		try {
			storage().remove(cacheLocation);
		} catch(IllegalArgumentException e) {
			//Already gone! Shouldn't need to fail.
		}
		
		//Mark children for deletion
		Vector<Resource> records = table.getResourcesForParent(r.getRecordGuid());
		for(Resource child : records) {
			Resource peer = incoming.getResourceWithId(child.getResourceId());
			if(peer != null && peer.getVersion() == child.getVersion()) {
				//Do nothing. Happy duplicates.
			} else {
				//Mark kid for deletion, it's no longer useful.
				table.commit(child, Resource.RESOURCE_STATUS_DELETE);
				//TODO: Write child back to table
			}
		}
		
		//CTS: The table should be taking care of this for the installer, no need to do it manually
		//Now remove yourself from the table
		//table.removeResource(r);
		
		return true;
	}
	
	public void cleanup() {
		if(cacheStorage != null) {
			cacheStorage.close();
		}
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		cacheLocation = ExtUtil.readInt(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, cacheLocation);
	}
	
	
	public boolean verifyInstallation(Resource r, Vector<UnresolvedResourceException> resources) {
		return false;
	}

}

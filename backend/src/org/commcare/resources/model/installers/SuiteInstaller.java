/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.commcare.reference.Reference;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Suite;
import org.commcare.xml.SuiteParser;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class SuiteInstaller implements ResourceInstaller {
	
	private IStorageUtility cacheStorage;
	
	private IStorageUtility storage() {
		if(cacheStorage == null) {
			cacheStorage = StorageManager.getStorage(Suite.STORAGE_KEY);
		}
		return cacheStorage;
	}
	
	int cacheLocation;
	
	public SuiteInstaller() {
		
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize() {
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

	
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) throws UnresolvedResourceException{ 
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
			//If it's in the cache, we should just get it from there
			return false;
		} else {
			SuiteParser parser = new SuiteParser(ref.getStream(), table, r.getRecordGuid());
			try {
				Suite s = parser.parse();
				storage().write(s);
				cacheLocation = s.getID();
				
				r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
				table.commit(r);
				
				//Add s to the RMS
				//Add a resource location for r for its cache location
				//so it can be uninstalled appropriately.
				return true;
			} catch (InvalidStructureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (StorageFullException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
	}
	
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
				child.setStatus(Resource.RESOURCE_STATUS_DELETE);
				table.commit(child);
				//TODO: Write child back to table
			}
		}
		
		//Now remove yourself from the table
		table.removeResource(r);
		
		return true;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		cacheLocation = ExtUtil.readInt(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, cacheLocation);
		
	}
}

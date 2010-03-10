/**
 * 
 */
package org.commcare.resources.model.installers;

import org.commcare.reference.Reference;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareManager;
import org.commcare.xml.SuiteParser;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.core.services.storage.StorageFullException;

/**
 * @author ctsims
 *
 */
public class SuiteInstaller extends CacheInstaller {
	
	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize() throws ResourceInitializationException {
		CommCareManager._().registerSuite(cacheLocation);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
	 */
	public boolean requiresRuntimeInitialization() {
		return true;
	}
	
	protected String getCacheKey() {
		return Suite.STORAGE_KEY;
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
				
				table.commit(r,Resource.RESOURCE_STATUS_INSTALLED);
				
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
}

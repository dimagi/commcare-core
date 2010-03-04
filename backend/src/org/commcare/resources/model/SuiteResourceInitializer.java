/**
 * 
 */
package org.commcare.resources.model;

import java.util.Hashtable;
import java.util.Vector;

import org.commcare.reference.Reference;
import org.commcare.suite.model.Suite;
import org.commcare.xml.SuiteParser;
import org.commcare.xml.util.InvalidStructureException;

/**
 * @author ctsims
 *
 */
public class SuiteResourceInitializer implements ResourceInstaller {
	
	private Hashtable<String, Suite> cache() {
		if(cache == null) {
			cache = new Hashtable<String, Suite>();
		}
		return cache;
	}
	public static Hashtable<String, Suite> cache;
	
	String cacheIdentifier;
	
	int recordId;

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initializeResource(Resource r) {
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

	
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) { 
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
			//If it's in the cache, we should just get it from there
			return false;
		} else {
			SuiteParser parser = new SuiteParser(ref.getStream(), table, r.getRecordGuid());
			try {
				Suite s = parser.parse();
				if(upgrade) {
					cacheIdentifier = r.getResourceId() + "_TEMP";
					cache().put(cacheIdentifier,s);
					r.setStatus(Resource.RESOURCE_STATUS_UPGRADE);
				} else {
					cacheIdentifier = r.getResourceId();
					cache().put(cacheIdentifier,s);
					r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
				}
				//Add s to the RMS
				//Add a resource location for r for its cache location
				//so it can be uninstalled appropriately.
				return true;
			} catch (InvalidStructureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
	}
	
	public boolean upgrade(Resource r) {
		Suite s = cache().get(cacheIdentifier);
		String old = cacheIdentifier;
		cacheIdentifier = cacheIdentifier.substring(0,cacheIdentifier.indexOf("_TEMP"));
		cache().put(cacheIdentifier, s);
		cache().remove(old);
		r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
		return true;
	}

	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) {
		//Previous failed uninstall might have wiped out this install, make sure we're 
		//still installed for real, then wipe out.
		if(cacheIdentifier != null) {
			//TODO: Dirty flag which marks indeterminate deletion
			cache.remove(cacheIdentifier);
			cacheIdentifier = null;
		}
		Vector<Resource> records = table.getResourcesForParent(r.getRecordGuid());
		for(Resource child : records) {
			Resource peer = incoming.getResourceWithId(child.getResourceId());
			if(peer != null && peer.getVersion() == child.getVersion()) {
				//Do nothing. Happy duplicates.
			} else {
				//Mark kid for deletion, it's no longer useful.
				child.setStatus(Resource.RESOURCE_STATUS_DELETE);
			}
		}
		return true;
	}
}

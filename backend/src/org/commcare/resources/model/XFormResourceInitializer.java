/**
 * 
 */
package org.commcare.resources.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import org.commcare.reference.Reference;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.XFormParser;

/**
 * @author ctsims
 *
 */
public class XFormResourceInitializer implements ResourceInstaller {
	
	private Hashtable<String, FormDef> cache() {
		if(cache == null) {
			cache = new Hashtable<String, FormDef>();
		}
		return cache;
	}
	public static Hashtable<String, FormDef> cache;
	
	String cacheIdentifier;

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initializeResource(Resource r) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
	 */
	public boolean requiresRuntimeInitialization() {
		return false;
	}

	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) { 
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
			//If it's in the cache, we should just get it from there
			return false;
		} else {
			InputStream input = ref.getStream();
			if(input == null) {
				return false;
			}
			FormDef formDef = XFormParser.getFormDef(new InputStreamReader(input));
			if(formDef == null) {
				//Bad Form!
				return false;
			}
			if(cache().containsKey(formDef.getInstance().schema)) {
				cacheIdentifier = formDef.getInstance().schema + "_TEMP";
				cache.put(cacheIdentifier,formDef);
				r.setStatus(Resource.RESOURCE_STATUS_UPGRADE);
			} else {
				cacheIdentifier = formDef.getInstance().schema;
				cache.put(cacheIdentifier,formDef);
				r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
			}
			//Add formDef to the RMS
			//Add a resource location for r for its cache location
			//so it can be uninstalled appropriately.
			return true;
		}
	}
	
	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) {
		table.removeResource(r);
		cache.remove(cacheIdentifier);
		return true;
	}
	
	public boolean upgrade(Resource r) {
		FormDef form = cache.get(cacheIdentifier);
		String n = cacheIdentifier.substring(0,cacheIdentifier.indexOf("_TEMP"));
		cache.put(n,form);
		cache.remove(cacheIdentifier);
		return true;
	}
}

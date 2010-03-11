/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xform.parse.XFormParser;

/**
 * @author ctsims
 *
 */
public class XFormInstaller extends CacheInstaller {

	protected String getCacheKey() {
		return FormDef.STORAGE_KEY;
	}

	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) throws UnresolvedResourceException {
		
		try {
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
			if(upgrade) {
				//There's already a record in the cache with this namespace, so we can't ovewrite it.
				//TODO: If something broke, this record might already exist. Might be worth checking.
				formDef.getInstance().schema = formDef.getInstance().schema + "_TEMP";
				storage().write(formDef);
				cacheLocation = formDef.getID();
				
				//Resource is installed and ready for upgrade 
				table.commit(r,Resource.RESOURCE_STATUS_UPGRADE);
			} else {
				storage().write(formDef);
				cacheLocation = formDef.getID();
				//Resource is fully installed
				table.commit(r,Resource.RESOURCE_STATUS_INSTALLED);
			}
			return true;
		}
		} catch (StorageFullException e) {
			
			//Couldn't install, no room left in storage.
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false; 
		}
	}
	
	public boolean upgrade(Resource r, ResourceTable table) throws UnresolvedResourceException {
		FormDef form = (FormDef)storage().read(cacheLocation);
		String tempString = form.getInstance().schema;
		form.getInstance().schema = tempString.substring(0,tempString.indexOf("_TEMP")); 
		try {
			storage().write(form);
		} catch (StorageFullException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}

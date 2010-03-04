/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import org.commcare.reference.Reference;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Suite;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xform.parse.XFormParser;

/**
 * @author ctsims
 *
 */
public class XFormInstaller implements ResourceInstaller {
	
	private IStorageUtility cacheStorage;
	
	private IStorageUtility storage() {
		if(cacheStorage == null) {
			cacheStorage = StorageManager.getStorage(FormDef.STORAGE_KEY);
		}
		return cacheStorage;
	}
	
	int cacheLocation;

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
	 */
	public boolean requiresRuntimeInitialization() {
		return false;
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
				r.setStatus(Resource.RESOURCE_STATUS_UPGRADE);
				table.commit(r);
			} else {
				storage().write(formDef);
				cacheLocation = formDef.getID();
				//Resource is fully installed
				r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
				table.commit(r);
			}
			return true;
		}
		} catch (StorageFullException e) {
			
			//Couldn't install, no room left in storage.
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) throws UnresolvedResourceException {
		storage().remove(cacheLocation);
		table.removeResource(r);
		return true;
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

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		cacheLocation = ExtUtil.readInt(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, cacheLocation);
	}
}

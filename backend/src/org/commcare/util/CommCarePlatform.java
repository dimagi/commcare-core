/**
 * 
 */
package org.commcare.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;

/**
 * TODO: This isn't really a great candidate for a 
 * singleton interface. It should almost certainly be
 * a more broad code-based installer/registration
 * process or something.
 * 
 * Also: It shares a lot of similarities with the 
 * Context app object in j2me. Maybe we should roll
 * some of that in.
 * 
 * @author ctsims
 *
 */
public class CommCarePlatform implements CommCareInstance {
	//TODO: We should make this unique using the parser to invalidate this ID or something
	private static final String APP_PROFILE_RESOURCE_ID = "commcare-application-profile";
	
	private Vector<Integer> suites;
	private int profile;
	
	private int majorVersion;
	private int minorVersion;
	
	public CommCarePlatform(int majorVersion, int minorVersion) {
		profile = -1;
		suites = new Vector<Integer>();
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}
	
	public void init(String profileReference, ResourceTable global, boolean forceInstall) throws UnfullfilledRequirementsException,  UnresolvedResourceException{
		try {

			if (!global.isReady()) {
				global.prepareResources(null, this);
			}
			
			// First, see if the appropriate profile exists
			Resource profile = global.getResourceWithId(APP_PROFILE_RESOURCE_ID);
			
			//If it does not, we need to grab it locally, and get parsing...
			if (profile == null) {

				Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
				locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_REMOTE, profileReference));
				
				//We need a way to identify this version...
				Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN, APP_PROFILE_RESOURCE_ID , locations);

				global.addResource(r, global.getInstallers().getProfileInstaller(forceInstall), "");
				global.prepareResources(null, this);
			} else{
				//Assuming it does exist, we might want to do an automatic
				//upgrade here, leaving that for a future date....
			}
		} catch (StorageFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getMajorVersion() {
		return majorVersion;
	}
	
	public int getMinorVersion() {
		return minorVersion;
	}
	
	public void upgrade(ResourceTable global, ResourceTable temporary) throws UnfullfilledRequirementsException {
		if (!global.isReady()) {
			throw new RuntimeException("The Global Resource Table was not properly made ready");
		}
		
		Profile current = getCurrentProfile();

		this.upgrade(global, temporary, current.getAuthReference());
	}
	
	
	public void upgrade(ResourceTable global, ResourceTable temporary, String profileReference) throws UnfullfilledRequirementsException {

		if (!global.isReady()) {
			throw new RuntimeException("The Global Resource Table was not properly made ready");
		}
		
		//In the future: Continuable upgrades. Now: Clear old upgrade info
		temporary.clear();

		Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
		locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, profileReference));
			
		//We need a way to identify this version...
		Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN, APP_PROFILE_RESOURCE_ID , locations);
		
		try {
			temporary.addResource(r, temporary.getInstallers().getProfileInstaller(false), null);
			temporary.prepareResources(global, this);
			global.upgradeTable(temporary);
			
			//Not implemented yet!
			//upgradeTable.destroy();
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new RuntimeException("Storage Full while trying to upgrade! Bad! Clear some room on the device and try again");
		} catch (UnresolvedResourceException e) {
			e.printStackTrace();
			throw new RuntimeException("A Resource couldn't be found while trying to upgrade!");
		}
		profile = -1;
		suites.removeAllElements();
		//Is it really possible to verify that we've un-registered everything here? Locale files are 
		//registered elsewhere, and we can't guarantee we're the only thing in there, so we can't
		//straight up clear it...
		
		initialize(global);
	}
	
	public Profile getCurrentProfile() {
		return (Profile)(StorageManager.getStorage(Profile.STORAGE_KEY).read(profile));
	}
	
	public Vector<Suite> getInstalledSuites() {
		Vector<Suite> installedSuites = new Vector<Suite>();
		IStorageUtility utility = StorageManager.getStorage(Suite.STORAGE_KEY);
		for(Integer i : suites) {
			installedSuites.addElement((Suite)(utility.read(i.intValue())));
		}
		return installedSuites;
	}
	
	public void setProfile(Profile p) {
		this.profile = p.getID();
	}
	
	
	public void registerSuite(Suite s) {
		this.suites.addElement(new Integer(s.getID()));
	}
	
	public void initialize(ResourceTable global) {
		try {
			global.initializeResources(this);
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing Resource! "+ e.getMessage());
		}
	}
	
	public Hashtable<String, Entry> getMenuMap() {
		Vector<Suite> installed = getInstalledSuites();
		Hashtable<String, Entry> merged = new Hashtable<String, Entry>();
		
		for(Suite s : installed) {
			Hashtable<String, Entry> table = s.getEntries();
			for(Enumeration en = table.keys() ; en.hasMoreElements() ; ) {
				String key = (String)en.nextElement();
				merged.put(key, table.get(key));
			}
		}
		return merged;
	}
	
	public static Vector<Resource> getResourceListFromProfile(ResourceTable master) {
		Vector<Resource> unresolved = new Vector<Resource>();
		Vector<Resource> resolved = new Vector<Resource>();
		Resource r = master.getResourceWithId(APP_PROFILE_RESOURCE_ID);
		if(r == null) {
			return resolved;
		}
		unresolved.addElement(r);
		while(unresolved.size() > 0) {
			Resource current = unresolved.firstElement();
			unresolved.removeElement(current);
			resolved.addElement(current);
			Vector<Resource> children = master.getResourcesForParent(current.getRecordGuid());
			for(Resource child : children) {
				unresolved.addElement(child);
			}
		}
		return resolved;
	}
}

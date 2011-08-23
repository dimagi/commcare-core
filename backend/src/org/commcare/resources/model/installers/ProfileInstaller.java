/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Profile;
import org.commcare.util.CommCareInstance;
import org.commcare.xml.ProfileParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class ProfileInstaller extends CacheInstaller {

	private static Hashtable<String, Profile> localTable;
	private boolean forceVersion;
	
	public ProfileInstaller() {
		forceVersion = false;
	}
	
	public ProfileInstaller(boolean forceVersion) {
		this.forceVersion = forceVersion;
	}
	
	private Hashtable<String, Profile> getlocal() {
		if(localTable == null) {
			localTable = new Hashtable<String, Profile>();
		}
		return localTable;
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize(CommCareInstance instance) throws ResourceInitializationException {
		//Certain properties may not have been able to set during install, so we'll make sure they're
		//set here.
		Profile p = (Profile)storage().read(cacheLocation);
		p.initializeProperties(false);
		
		instance.setProfile(p);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
	 */
	public boolean requiresRuntimeInitialization() {
		return true;
	}
	
	protected String getCacheKey() {
		return Profile.STORAGE_KEY;
	}
	
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException{
		//Install for the profile installer is a two step process. Step one is to parse the file and read the relevant data.
		//Step two is to actually install the resource if it needs to be (whether or not it should will be handled
		//by the resource table).
		
		//If we've already got the local copy, and the installer is marked as such, install and roll out.
		try {
			if(getlocal().containsKey(r.getRecordGuid()) && r.getStatus() == Resource.RESOURCE_STATUS_LOCAL) {
				Profile local = getlocal().get(r.getRecordGuid());
				installInternal(local);
				table.commit(r, Resource.RESOURCE_STATUS_UPGRADE);
				localTable.remove(r.getRecordGuid());
			
				for(Resource child : table.getResourcesForParent(r.getRecordGuid())) {
					table.commit(child,Resource.RESOURCE_STATUS_UNINITIALIZED);
				}
				return true;
			}
		
		//Otherwise we need to get the profile from its location, parse it out, and 
		//set the relevant parameters.
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
			//If it's in the cache, we should just get it from there
			return false;
		} else {
			ProfileParser parser = new ProfileParser(ref.getStream(), instance, table, r.getRecordGuid(), 
					upgrade ? Resource.RESOURCE_STATUS_PENDING : Resource.RESOURCE_STATUS_UNINITIALIZED, forceVersion);
				if(Resource.RESOURCE_AUTHORITY_REMOTE == location.getAuthority()) {
					parser.setMaximumAuthority(Resource.RESOURCE_AUTHORITY_REMOTE);
				}
				Profile p = parser.parse();
				
				//If we're upgrading we need to come back and see if the statuses need to change
				if(upgrade) {
					getlocal().put(r.getRecordGuid(), p);
					table.commit(r,Resource.RESOURCE_STATUS_LOCAL, p.getVersion());
				} else {
					p.initializeProperties(true);
					installInternal(p);
					//TODO: What if this fails? Maybe we should be throwing exceptions...
					table.commit(r, Resource.RESOURCE_STATUS_INSTALLED, p.getVersion());
				}
				
				return true;
		}
		} catch (InvalidStructureException e) {
			e.printStackTrace();
			return false;
		} catch (StorageFullException e) {
			e.printStackTrace();
			return false;
		}  catch (IOException e) {
			e.printStackTrace();
			return false; 
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	private void installInternal(Profile profile) throws StorageFullException {
			storage().write(profile);
			cacheLocation = profile.getID();
	}
	
	public boolean upgrade(Resource r, ResourceTable table) throws UnresolvedResourceException {
		Profile p;
		if(getlocal().containsKey(r.getRecordGuid())) {
			p = getlocal().get(r.getRecordGuid());
		} else {
			p = (Profile)storage().read(cacheLocation);
		}
		p.initializeProperties(true);
		try {
			storage().write(p);
			return true;
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new UnresolvedResourceException(r,"Couldn't write the profile to storage. Full.");
		}
	}
	
	public void cleanup() {
		super.cleanup();
		if(localTable != null) {
			localTable.clear();
			localTable = null;
		}
	}
	
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		super.readExternal(in, pf);
		forceVersion = ExtUtil.readBool(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		super.writeExternal(out);
		ExtUtil.writeBool(out, forceVersion);
	}
}

/**
 * 
 */
package org.commcare.resources.model;

import java.io.IOException;
import java.io.InputStream;

import org.commcare.reference.InvalidReferenceException;
import org.commcare.reference.Reference;
import org.commcare.reference.ReferenceUtil;
import org.javarosa.core.services.locale.LocalizationUtils;

/**
 * @author ctsims
 *
 */
public class LocaleFileResourceInitializer implements ResourceInstaller {
	
	String locale;
	String localReference;

	public LocaleFileResourceInitializer(String locale) {
		this.locale = locale;
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initializeResource(Resource r) {
		//TODO: Set 'r' status as error on error?
			InputStream is = null;
			try {
				is = r.OpenStream();
				LocalizationUtils.parseLocaleInput(ReferenceUtil.DeriveReference(localReference).getStream());
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (InvalidReferenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} finally {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e) {
						//Is this actually a problem? Who knows...
						e.printStackTrace();
					}
				}
			}
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#requiresRuntimeInitialization()
	 */
	public boolean requiresRuntimeInitialization() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#resourceReady(org.commcare.resources.model.Resource)
	 */
	public boolean resourceReady(Resource r) {
		return false;
	}
	
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, boolean upgrade) {
		//If we have local resource authority, and the file exists, things are golden. We can just use that file.
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
			if(ref.doesBinaryExist()) {
				localReference = ref.getURI();
				r.setStatus(Resource.RESOURCE_STATUS_INSTALLED);
				return true;
			} else {
				//If the file isn't there, not much we can do about it.
				return false;
			}
		} else if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
			//We need to download the resource, and store it locally. Either in the cache
			//(if no resource location is available) or in a local reference if one exists.
			InputStream incoming = ref.getStream();
			if(incoming == null) {
				//if it turns out there isn't actually a remote resource, bail.
				return false;
			}
			
			//TODO: Implement local cache code
			return false;
		}
		return false;
	}
	public boolean upgrade(Resource r) {
		return true;
	}

	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) {
		return true;
	}

}

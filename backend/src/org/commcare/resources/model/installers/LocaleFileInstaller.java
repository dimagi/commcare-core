/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceInstaller;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceDataSource;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.StreamUtil;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class LocaleFileInstaller implements ResourceInstaller<CommCareInstance> {
	
	String locale;
	String localReference;
	
	OrderedHashtable cache;
	
	/**
	 * Serialization only!
	 */
	public LocaleFileInstaller() {
		
	}

	public LocaleFileInstaller(String locale) {
		this.locale = locale;
		this.localReference = "";
	}
	
	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize(CommCareInstance instance) throws ResourceInitializationException {
		if(cache == null) {
			Localization.registerLanguageReference(locale, localReference);
		} else {
			//TODO: This will _not_ create a locale's availability if it doesn't exist. Need to determine
			//what to do about that... 
			Localization.getGlobalLocalizerAdvanced().registerLocaleResource(locale, new TableLocaleSource(cache));
		}
		return true;
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
	
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance,  boolean upgrade) throws UnresolvedResourceException {
		//If we have local resource authority, and the file exists, things are golden. We can just use that file.
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_LOCAL) {
			try {
				if(ref.doesBinaryExist()) {
					localReference = ref.getURI();
					table.commit(r,Resource.RESOURCE_STATUS_INSTALLED);
					return true;
				} else {
					//If the file isn't there, not much we can do about it.
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
			//We need to download the resource, and store it locally. Either in the cache
			//(if no resource location is available) or in a local reference if one exists.
			try {
				if(!ref.doesBinaryExist()) {
					return false;
				}
				InputStream incoming = ref.getStream();
				if(incoming == null) {
					//if it turns out there isn't actually a remote resource, bail.
					return false;
				}
				
				//Now we're gong to try to find a local location to put the resource.
				//Start with an arbitrary file location (since we don't support destination
				//information yet, which we probably should soon).
				String uri = ref.getURI();
				int lastslash = uri.lastIndexOf('/');
				//Now we have a local part reference
				uri = uri.substring(lastslash == -1 ? 0 : lastslash);
				int copy = 0; 
				
				try {
					Reference destination = ReferenceManager._().DeriveReference("jr://file/" + uri);
					if(destination.isReadOnly()) {
						return cache(incoming, r, table);
						
					}
					while(destination.doesBinaryExist()) {
						//Need a different location.
						copy++;
						String newUri = uri + "." + copy;
						destination = ReferenceManager._().DeriveReference("jr://file/" + newUri);
					}
					
					//destination is now a valid local reference, so we can store the file there.
					
					OutputStream output = destination.getOutputStream();
					StreamUtil.transfer(incoming, output);
					incoming.close();
					output.close();
					
					this.localReference = destination.getURI();
					if(upgrade) {
						table.commit(r,Resource.RESOURCE_STATUS_UPGRADE);
					} else {
						table.commit(r,Resource.RESOURCE_STATUS_INSTALLED);
					}
					return true;
					
				} catch (InvalidReferenceException e) {
					return cache(incoming, r, table);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false; 
			}
			
			//TODO: Implement local cache code
			//	return false;
		}
		return false;
	}
	private boolean cache(InputStream incoming, Resource r, ResourceTable table) throws UnresolvedResourceException {
		try {
			cache = LocalizationUtils.parseLocaleInput(incoming);
			table.commit(r,Resource.RESOURCE_STATUS_INSTALLED);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean upgrade(Resource r, ResourceTable table) throws UnresolvedResourceException {
		//TODO: Rename file to take off ".N"?
		return true;
	}

	public boolean uninstall(Resource r, ResourceTable table, ResourceTable incoming) throws UnresolvedResourceException {
		//If we're not using files, just deal with the cache (this is even likely unecessary).
		if(cache != null) {
			cache.clear();
			cache = null;
			return true;
		}
		Reference reference;
		try {
			reference = ReferenceManager._().DeriveReference(localReference);
			if(!reference.isReadOnly()) {
				reference.remove();
			}
			table.removeResource(r);
			return true;
		} catch (InvalidReferenceException e) {
			e.printStackTrace();
			throw new UnresolvedResourceException(r,"Could not resolve locally installed reference at" + localReference);
		} catch (IOException e) {
			e.printStackTrace();
			throw new UnresolvedResourceException(r,"Problem removing local data at reference " + localReference);
		}
	}
	
	public void cleanup() {
		
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		locale = ExtUtil.readString(in);
		localReference = ExtUtil.readString(in);
		cache = (OrderedHashtable)ExtUtil.nullIfEmpty((OrderedHashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class, true), pf));
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, locale);
		ExtUtil.writeString(out, localReference);
		ExtUtil.write(out, new ExtWrapMap(ExtUtil.emptyIfNull(cache)));
	}

}

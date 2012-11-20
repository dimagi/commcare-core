/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.PrefixTreeNode;
import org.javarosa.core.util.SizeBoundVector;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.parse.XFormParser;

/**
 * @author ctsims
 *
 */
public class XFormInstaller extends CacheInstaller {

	protected String getCacheKey() {
		return FormDef.STORAGE_KEY;
	}

	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance,  boolean upgrade) throws UnresolvedResourceException {
		InputStream incoming = null;
		try {
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
			//If it's in the cache, we should just get it from there
			return false;
		} else {
			incoming = ref.getStream();
			if(incoming == null) {
				return false;
			}
			FormDef formDef = new XFormParser(new InputStreamReader(incoming, "UTF-8")).parse();
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
			throw new UnreliableSourceException(r, e.getMessage());
		} catch(XFormParseException xpe ) {
			xpe.printStackTrace();
			return false;
		} finally {
			try { if(incoming != null) { incoming.close(); } } catch (IOException e) {}
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
	
	public boolean verifyInstallation(Resource r, Vector<UnresolvedResourceException> problems) {
		
		SizeBoundVector sizeBoundProblems = (SizeBoundVector) problems;
		
		//Check to see whether the formDef exists and reads correctly
		FormDef formDef;
		try {
			formDef = (FormDef)storage().read(cacheLocation);
		} catch(Exception e) {
			sizeBoundProblems.addElement(new UnresolvedResourceException(r, "Form did not properly save into persistent storage"));
			return true;
		}
		//Otherwise, we want to figure out if the form has media, and we need to see whether it's properly
		//available
		Localizer localizer = formDef.getLocalizer();
		//get this out of the memory ASAP!
		formDef = null;
		if(localizer == null) {
			//things are fine
			return false;
		}
		int missingVideoCount = 0;
		int missingImageCount = 0;
		int missingAudioCount = 0;
		for(String locale : localizer.getAvailableLocales()) {
			OrderedHashtable<String, PrefixTreeNode> localeData = localizer.getLocaleData(locale);
			for(Enumeration en = localeData.keys(); en.hasMoreElements() ; ) {
				String key = (String)en.nextElement();
				if(key.indexOf(";") != -1) {
					//got some forms here
					String form = key.substring(key.indexOf(";") + 1, key.length());
					if(form.equals(FormEntryCaption.TEXT_FORM_VIDEO) || 
					   form.equals(FormEntryCaption.TEXT_FORM_AUDIO) || 
					   form.equals(FormEntryCaption.TEXT_FORM_IMAGE)) {
						try {
							String externalMedia = localeData.get(key).render();
							Reference ref = ReferenceManager._().DeriveReference(externalMedia);
							String localName = ref.getLocalURI();
							try {
								if(!ref.doesBinaryExist()) {
									sizeBoundProblems.addElement(new UnresolvedResourceException(r,localName));
									if(form.equals(FormEntryCaption.TEXT_FORM_VIDEO)){sizeBoundProblems.addBadVideoReference();}
									if(form.equals(FormEntryCaption.TEXT_FORM_AUDIO)){sizeBoundProblems.addBadAudioReference();}
									if(form.equals(FormEntryCaption.TEXT_FORM_IMAGE)){sizeBoundProblems.addBadImageReference();}
								}
							} catch (IOException e) {
								sizeBoundProblems.addElement(new UnresolvedResourceException(r,"Problem reading external media: " + localName));
							}
						} catch (InvalidReferenceException e) {
							//So the problem is that this might be a valid entry that depends on context
							//in the form, so we'll ignore this situation for now.
						}
					}
				}
			}
		}
		if(sizeBoundProblems.size() == 0 ) { return false;}
		return true;
	}
}

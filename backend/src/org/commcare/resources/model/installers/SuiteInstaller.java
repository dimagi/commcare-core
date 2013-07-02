/**
 * 
 */
package org.commcare.resources.model.installers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCareInstance;
import org.commcare.xml.SuiteParser;
import org.commcare.xml.util.InvalidStructureException;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.SizeBoundUniqueVector;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class SuiteInstaller extends CacheInstaller<Suite> {
	
	/* (non-Javadoc)
	 * @see org.commcare.resources.model.ResourceInitializer#initializeResource(org.commcare.resources.model.Resource)
	 */
	public boolean initialize(CommCareInstance instance) throws ResourceInitializationException {
		instance.registerSuite((Suite)storage().read(cacheLocation));
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
	
	public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance,  boolean upgrade) throws UnresolvedResourceException, UnfullfilledRequirementsException{
		if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
			//If it's in the cache, we should just get it from there
			return false;
		} else {
			
			InputStream incoming = null;
			try {
				incoming = ref.getStream();
				SuiteParser parser = new SuiteParser(incoming, table, r.getRecordGuid());
				if(location.getAuthority() == Resource.RESOURCE_AUTHORITY_REMOTE) {
					parser.setMaximumAuthority(Resource.RESOURCE_AUTHORITY_REMOTE);
				}
				Suite s = parser.parse();
				storage().write(s);
				cacheLocation = s.getID();
				
				table.commit(r,Resource.RESOURCE_STATUS_INSTALLED);
				
				//TODOD:
				//Add a resource location for r for its cache location
				//so it can be uninstalled appropriately.
				return true;
			} catch (InvalidStructureException e) {
				throw new UnresolvedResourceException(r, e.getMessage(), true);
			} catch (StorageFullException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				throw new UnreliableSourceException(r, e.getMessage()); 
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} finally {
				try { if(incoming != null) { incoming.close(); } } catch (IOException e) {}
			}
		}
	}
	
	private void checkMedia(String filePath, SizeBoundUniqueVector<MissingMediaException> problems){
		try{
			Reference ref = ReferenceManager._().DeriveReference(filePath);
			String localName = ref.getLocalURI();
			try {
				if(!ref.doesBinaryExist()) {
					problems.addElement(new MissingMediaException(null,"Missing external media: " + localName, filePath));
					problems.addBadImageReference();
				}
			} catch (IOException e) {
				problems.addElement(new MissingMediaException(null,"Problem reading external media: " + localName, filePath));
			} 
		} catch (InvalidReferenceException e) {
			//So the problem is that this might be a valid entry that depends on context
			//in the form, so we'll ignore this situation for now.
		}
	}
	
	public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problems) {
		
		SizeBoundUniqueVector sizeBoundProblems = (SizeBoundUniqueVector) problems;
		
		checkMedia(Localization.get("icon.demo.path"), sizeBoundProblems);
		checkMedia(Localization.get("icon.login.path"), sizeBoundProblems);
		
		//Check to see whether the formDef exists and reads correctly
		Suite mSuite;
		try {
			mSuite = (Suite)storage().read(cacheLocation);
		} catch(Exception e) {
			sizeBoundProblems.addElement(new MissingMediaException(r, "Suite did not properly save into persistent storage"));
			return true;
		}
		//Otherwise, we want to figure out if the form has media, and we need to see whether it's properly
		//available
		try{
			Hashtable<String,Entry> mHashtable = mSuite.getEntries();
			for(Enumeration en = mHashtable.keys();en.hasMoreElements() ; ){
				String key = (String)en.nextElement();
			}
			Vector<Menu> menus = mSuite.getMenus();
			Iterator e = menus.iterator();
			
			int missingAURI = 0;
			int missingIURI= 0;
			
			while(e.hasNext()){
				Menu mMenu = (Menu)e.next();

				String aURI = mMenu.getAudioURI();
				String iURI = mMenu.getImageURI();
				
				if(aURI != null){
					Reference aRef = ReferenceManager._().DeriveReference(aURI);
					String aLocalName = aRef.getLocalURI();				
					if(!aRef.doesBinaryExist()) {
						sizeBoundProblems.addElement(new MissingMediaException(r,aLocalName));
						sizeBoundProblems.addBadAudioReference();
						missingAURI++;
					}
				}
				if(iURI != null){
					Reference iRef = ReferenceManager._().DeriveReference(iURI);
					String iLocalName = iRef.getLocalURI();					
					if(!iRef.doesBinaryExist()) {
						sizeBoundProblems.addElement(new MissingMediaException(r,iLocalName));
						sizeBoundProblems.addBadImageReference();
						missingIURI++;
					}
				}
			}
		}
		catch(Exception exc){
			System.out.println("fail: " + exc.getMessage());
			System.out.println("fail: " + exc.toString());
		}
		if(problems.size() == 0 ) { return false;}
		return true;
	}
}

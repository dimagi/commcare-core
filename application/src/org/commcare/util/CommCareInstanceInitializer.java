/**
 * 
 */
package org.commcare.util;

import java.util.Vector;

import org.javarosa.cases.instance.CaseInstanceTreeElement;
import org.javarosa.cases.model.Case;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.user.model.User;

/**
 * @author ctsims
 *
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {
	CommCareSessionController session;
	CaseInstanceTreeElement casebase;
	
	public CommCareInstanceInitializer(){ 
		this(null);
	}
	public CommCareInstanceInitializer(CommCareSessionController session) {
		this.session = session;
	}
	
	public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
		String ref = instance.getReference();
		if(ref.indexOf("case") != -1) {
			if(casebase == null) {
				casebase =  new CaseInstanceTreeElement(instance.getBase(), (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY));
			} else {
				casebase.rebase(instance.getBase());
			}
			return casebase;
		}
		if(instance.getReference().indexOf("fixture") != -1) {
			String refId = ref.substring(ref.lastIndexOf('/') + 1, ref.length());
			IStorageUtilityIndexed storage = (IStorageUtilityIndexed)StorageManager.getStorage("fixture");
			
			FormInstance fixture = null;
			Vector<Integer> relevantFixtures = storage.getIDsForValue(FormInstance.META_ID, refId);
			
			///... Nooooot so clean.
			if(relevantFixtures.size() == 1) {
				//easy case, one fixture, use it
				fixture = (FormInstance)storage.read(relevantFixtures.elementAt(0).intValue());
				//TODO: Userid check anyway?
			} else if(relevantFixtures.size() > 1){
				//intersect userid and fixtureid set.
				//TODO: Replace context call here with something from the session, need to stop relying on that coupling
				String userId = "";
				User u = CommCareContext._().getUser();
				if(u != null) {
					userId = u.getUniqueId();
				}
				Vector<Integer> relevantUserFixtures = storage.getIDsForValue(FormInstance.META_XMLNS, userId);
				
				if(relevantUserFixtures.size() != 0) {
					Integer userFixture = ArrayUtilities.intersectSingle(relevantFixtures, relevantUserFixtures);
					if(userFixture != null) {
						fixture = (FormInstance)storage.read(userFixture.intValue());
					}
				}
				if(fixture == null) {
					//Oooookay, so there aren't any fixtures for this user, see if there's a global fixture.				
					Integer globalFixture = ArrayUtilities.intersectSingle(storage.getIDsForValue(FormInstance.META_XMLNS, ""), relevantFixtures);
					if(globalFixture == null) {
						//No fixtures?! What is this. Fail somehow. This method should really have an exception contract.
						throw new RuntimeException("Could not find an appropriate filter for src: " + instance.getReference());
					}
					fixture = (FormInstance)storage.read(globalFixture.intValue());
				}
			} else {
				//No fixtures?! What is this. Fail somehow. This method should really have an exception contract.
				throw new RuntimeException("Could not find an appropriate filter for src: " + instance.getReference());
			}
			
			//FormInstance fixture = (FormInstance)storage.getRecordForValue(FormInstance.META_ID, refId);
			TreeElement root = fixture.getRoot();
			root.setParent(instance.getBase());
			return root;
		}
		if(instance.getReference().indexOf("session") != -1) {
			TreeElement root = session.getSessionInstance().getRoot();
			root.setParent(instance.getBase());
			return root;
		}
		return null;
	}
}

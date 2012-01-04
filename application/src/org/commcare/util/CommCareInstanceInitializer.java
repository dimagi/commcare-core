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
			String userId = "";
			User u = CommCareContext._().getUser();
			if(u != null) {
				userId = u.getUniqueId();
			}
			FormInstance fixture = CommCareUtil.loadFixtureForUser(ref.substring(ref.lastIndexOf('/') + 1, ref.length()), userId);
			if(fixture == null) {
				throw new RuntimeException("Could not find an appropriate fixture for src: " + ref);
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

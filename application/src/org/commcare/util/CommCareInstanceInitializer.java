/**
 * 
 */
package org.commcare.util;

import org.javarosa.cases.instance.CaseInstanceTreeElement;
import org.javarosa.cases.model.Case;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;

/**
 * @author ctsims
 *
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {
		public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
			String ref = instance.getReference();
			if(ref.indexOf("case") != -1) {
				return new CaseInstanceTreeElement(instance.getBase(), (IStorageUtilityIndexed)StorageManager.getStorage(Case.STORAGE_KEY));
			}
			if(instance.getReference().indexOf("fixture") != -1) {
				String refId = ref.substring(ref.lastIndexOf('/') + 1, ref.length());
				IStorageUtilityIndexed storage = (IStorageUtilityIndexed)StorageManager.getStorage("fixture");
				FormInstance fixture = (FormInstance)storage.getRecordForValue(FormInstance.META_ID, refId);
				TreeElement root = fixture.getRoot();
				root.setParent(instance.getBase());
				return root;
			}
			return null;
		}
}

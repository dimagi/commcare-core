/**
 * 
 */
package org.commcare.util;

import org.javarosa.cases.model.Case;
import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.util.restorable.IRecordFilter;
import org.javarosa.core.model.util.restorable.Restorable;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.user.model.User;

/**
 * @author Clayton Sims
 * @date Apr 28, 2009 
 *
 */
public class CommCareBackupRestoreSnapshot implements Restorable {

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.util.restorable.Restorable#exportData()
	 */
	public FormInstance exportData() {
		FormInstance everything = RestoreUtils.createRootDataModel(this);
		
		RestoreUtils.addData(everything, "cc-version", CommCareUtil.getVersion());
		RestoreUtils.addData(everything, "properties/device-id", PropertyManager._().getSingularProperty("DeviceID"));
		RestoreUtils.addData(everything, "properties/post-url", PropertyManager._().getSingularProperty("PostURL"));
		
		RestoreUtils.exportRMS(everything, Case.class, "cases", StorageManager.getStorage(Case.STORAGE_KEY), null);
		
		RestoreUtils.exportRMS(everything, User.class, "users", StorageManager.getStorage(User.STORAGE_KEY), new IRecordFilter () {
			public boolean filter(Object o) {
				User u = (User)o;
				return !(u.getID() == 1 && User.ADMINUSER.equals(u.getUserType()));
			}
		});

		RestoreUtils.exportRMS(everything, PatientReferral.class, "referrals", StorageManager.getStorage(PatientReferral.STORAGE_KEY), null);
		
		/*FormInstanceRMSUtility dmrms = (FormInstanceRMSUtility)JavaRosaServiceProvider.instance().getStorageManager().getRMSStorageProvider().getUtility(FormInstanceRMSUtility.getUtilityName());
		final Vector unsentIDs = getUnsentModelIDs(dmrms);
		RestoreUtils.exportRMS(everything, FormInstance.class, "forms", dmrms, new IRecordFilter () {
			public boolean filter(Object o) {
				return unsentIDs.contains(new Integer(((FormInstance)o).getId()));
			}
		});	*/	 
		
		return everything;

	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.util.restorable.Restorable#getRestorableType()
	 */
	public String getRestorableType() {
		return "commcare-brac-snapshot";
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.util.restorable.Restorable#importData(org.javarosa.core.model.instance.FormInstance)
	 */
	public void importData(FormInstance dm) {
		PropertyManager._().setProperty("DeviceID", (String)RestoreUtils.getValue("properties/device-id", dm));
		PropertyManager._().setProperty("PostURL", (String)RestoreUtils.getValue("properties/post-url", dm));

		RestoreUtils.importRMS(dm, StorageManager.getStorage(Case.STORAGE_KEY), Case.class, "cases");
		RestoreUtils.importRMS(dm, StorageManager.getStorage(User.STORAGE_KEY), User.class, "users");
		RestoreUtils.importRMS(dm, StorageManager.getStorage(PatientReferral.STORAGE_KEY), PatientReferral.class, "referrals");
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.util.restorable.Restorable#templateData(org.javarosa.core.model.instance.FormInstance, org.javarosa.core.model.instance.TreeReference)
	 */
	public void templateData(FormInstance dm, TreeReference parentRef) {
		RestoreUtils.applyDataType(dm, "cc-version", parentRef, String.class);
		RestoreUtils.applyDataType(dm, "properties/device-id", parentRef, String.class);
		RestoreUtils.applyDataType(dm, "properties/post-url", parentRef, String.class);

		RestoreUtils.templateChild(dm, "cases", parentRef, new Case());
		RestoreUtils.templateChild(dm, "users", parentRef, new User());
		RestoreUtils.templateChild(dm, "referrals", parentRef, new PatientReferral());
		//RestoreUtils.templateChild(dm, "forms", parentRef, new FormInstance());

	}

}

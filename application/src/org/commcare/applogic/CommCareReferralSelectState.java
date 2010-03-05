package org.commcare.applogic;

import org.javarosa.chsreferral.model.PatientReferral;
import org.javarosa.chsreferral.util.ReferralEntity;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.api.EntitySelectState;
import org.javarosa.entity.model.view.EntitySelectView;

public abstract class CommCareReferralSelectState extends EntitySelectState<PatientReferral> {

	ReferralEntity entity;
	
	public CommCareReferralSelectState (ReferralEntity entity) {
		this.entity = entity;
	}
	
	protected EntitySelectController<PatientReferral> getController () {
		return new EntitySelectController<PatientReferral>("Pending Referrals",
			   StorageManager.getStorage(PatientReferral.STORAGE_KEY), entity,
			   EntitySelectView.NEW_DISALLOWED, true, false);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.activity.EntitySelectTransitions#empty()
	 */
	public void empty() {
		throw new RuntimeException("transition not applicable");
	}

	/* (non-Javadoc)
	 * @see org.javarosa.entity.activity.EntitySelectTransitions#newEntity()
	 */
	public void newEntity() {
		throw new RuntimeException("transition not applicable");
	}
}

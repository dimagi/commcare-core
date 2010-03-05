package org.commcare.applogic;

import org.javarosa.cases.model.Case;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.api.EntitySelectState;
import org.javarosa.entity.model.Entity;
import org.javarosa.entity.model.view.EntitySelectView;

public abstract class CommCareCaseSelectState extends EntitySelectState<Case> {

	Entity<Case> entity;
	
	public CommCareCaseSelectState (Entity<Case> entity) {
		this.entity = entity;
	}
	
	protected EntitySelectController<Case> getController() {
		return new EntitySelectController<Case>("Choose " + entity.entityType(),
			StorageManager.getStorage(Case.STORAGE_KEY), entity, EntitySelectView.NEW_DISALLOWED, true, false);
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

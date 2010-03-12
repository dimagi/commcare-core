package org.commcare.applogic;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.api.EntitySelectState;
import org.javarosa.entity.model.Entity;
import org.javarosa.entity.model.view.EntitySelectView;

public abstract class CommCareSelectState<E extends Persistable> extends EntitySelectState<E> {

	Entity<E> entity;
	String storageKey;
	
	public CommCareSelectState (Entity<E> entity, String storageKey) {
		this.entity = entity;
		this.storageKey = storageKey;
	}
	
	protected EntitySelectController<E> getController () {
		return new EntitySelectController<E>(entity.entityType(),
			   StorageManager.getStorage(storageKey), entity,
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

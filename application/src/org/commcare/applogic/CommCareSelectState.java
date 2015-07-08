package org.commcare.applogic;

import org.commcare.util.CommCareEntitySelectController;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.entity.api.EntitySelectController;
import org.javarosa.entity.api.EntitySelectState;
import org.javarosa.entity.model.Entity;
import org.javarosa.entity.model.EntitySet;
import org.javarosa.entity.model.StorageEntitySet;
import org.javarosa.entity.model.view.EntitySelectView;
import org.javarosa.j2me.view.ProgressIndicator;

public abstract class CommCareSelectState<E> extends EntitySelectState<E> {

    Entity<E> entity;
    EntitySelectController<E> controller;

    public CommCareSelectState (Entity<E> entity, String storageKey) {
        this(entity, new StorageEntitySet(StorageManager.getStorage(storageKey)));
    }

    public CommCareSelectState (Entity<E> entity, EntitySet<E> set) {
        this.entity = entity;

        controller = new CommCareEntitySelectController<E>(entity.entityType(), set, entity,
                   EntitySelectView.NEW_DISALLOWED, true, false);
    }

    protected EntitySelectController<E> getController () {
        return controller;
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

    public ProgressIndicator getProgressIndicator() {
        return controller;
    }
}

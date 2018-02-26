package org.commcare.util;

import org.commcare.cases.entity.Entity;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by willpride on 2/26/18.
 */

public interface EntityProvider {
    Entity<TreeReference> getEntity(int index);
}

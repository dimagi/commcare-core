/**
 *
 */
package org.commcare.entity;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.Persistable;

/**
 * @author ctsims
 *
 */
public class InstanceEntityFilter<E extends Persistable> extends EntityFilter<E> {

    private FormInstanceLoader loader;
    private FormInstance template;

    public InstanceEntityFilter(FormInstanceLoader<E> loader, FormInstance template) {
        this.loader = loader;
        this.template = template;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.EntityFilter#matches(java.lang.Object)
     */
    public boolean matches(E e) {
        return true;
    }

}

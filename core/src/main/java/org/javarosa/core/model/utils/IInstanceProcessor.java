package org.javarosa.core.model.utils;

import org.javarosa.core.model.instance.FormInstance;

/**
 * An interface for classes which are capable of parsing and performing actions
 * on Data Model objects.
 *
 * @author Clayton Sims
 * @date Jan 27, 2009
 */
public interface IInstanceProcessor {

    /**
     * Processes the provided data model.
     *
     * @param tree The data model that will be handled.
     */
    public void processInstance(FormInstance tree);
}

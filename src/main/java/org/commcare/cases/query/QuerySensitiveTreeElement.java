package org.commcare.cases.query;

import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;

/**
 * NOTE: Each time a method is implemented here, it should be added to QuerySensitiveTreeElementWrapper
 *
 * Created by ctsims on 9/19/2017.
 */

public interface QuerySensitiveTreeElement<T extends AbstractTreeElement> extends AbstractTreeElement<T> {

    /**
     * Retrieves the TreeElement representing the attribute at
     * the provided namespace and name, or null if none exists.
     *
     * If 'null' is provided for the namespace, it will match the first
     * attribute with the matching name.
     */
    T getAttribute(QueryContext context, String namespace, String name);

    int getChildMultiplicity(QueryContext context, String name);

    /**
     * Get a child element with the given name and occurence position (multiplicity)
     *
     * @param name         the name of the child element to select
     * @param multiplicity is the n-th occurence of an element with a given name
     */
    T getChild(QueryContext context, String name, int multiplicity);

    boolean hasChildren(QueryContext context);

    TreeReference getRef(QueryContext context);
}

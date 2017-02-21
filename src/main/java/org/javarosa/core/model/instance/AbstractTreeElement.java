package org.javarosa.core.model.instance;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.Vector;

public interface AbstractTreeElement<T extends AbstractTreeElement> {

    boolean isLeaf();

    boolean isChildable();

    String getInstanceName();

    /**
     * Get a child element with the given name and occurence position (multiplicity)
     *
     * @param name         the name of the child element to select
     * @param multiplicity is the n-th occurence of an element with a given name
     */
    T getChild(String name, int multiplicity);

    /**
     * Get all the child nodes of this element, with specific name
     */
    Vector<T> getChildrenWithName(String name);

    boolean hasChildren();

    int getNumChildren();

    T getChildAt(int i);

    boolean isRepeatable();

    boolean isAttribute();

    int getChildMultiplicity(String name);

    /**
     * Visitor pattern acceptance method.
     *
     * @param visitor The visitor traveling this tree
     */
    void accept(ITreeVisitor visitor);

    /**
     * Returns the number of attributes of this element.
     */
    int getAttributeCount();

    /**
     * get namespace of attribute at 'index' in the vector
     */
    String getAttributeNamespace(int index);

    /**
     * get name of attribute at 'index' in the vector
     */
    String getAttributeName(int index);

    /**
     * get value of attribute at 'index' in the vector
     */
    String getAttributeValue(int index);

    /**
     * Retrieves the TreeElement representing the attribute at
     * the provided namespace and name, or null if none exists.
     *
     * If 'null' is provided for the namespace, it will match the first
     * attribute with the matching name.
     */
    T getAttribute(String namespace, String name);

    /**
     * get value of attribute with namespace:name' in the vector
     */
    String getAttributeValue(String namespace, String name);

    //return the tree reference that corresponds to this tree element
    TreeReference getRef();

    String getName();

    int getMult();

    //Support?
    AbstractTreeElement getParent();

    IAnswerData getValue();

    int getDataType();

    boolean isRelevant();

    String getNamespace();

    /**
     * TODO: Worst method name ever. Don't use this unless you know what's up.
     * @param predicates  possibly list of predicates to be evaluated. predicates will be removed from list if they are
     *                    able to be evaluated
     *
     */
    Collection<TreeReference> tryBatchChildFetch(String name, int mult,
                                                 Vector<XPathExpression> predicates,
                                                 EvaluationContext evalContext);
}

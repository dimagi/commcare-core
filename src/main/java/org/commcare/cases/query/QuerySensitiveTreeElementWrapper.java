package org.commcare.cases.query;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.Vector;

/**
 * Wraps an abstract tree element to provide no-op dispatch implementations for query sensitive
 * methods
 *
 *
 * Created by ctsims on 9/19/2017.
 */

public class QuerySensitiveTreeElementWrapper<T extends AbstractTreeElement> implements AbstractTreeElement<T>{
    QuerySensitiveTreeElement<T> wrapped;
    QueryContext context;

    public static <T extends AbstractTreeElement> AbstractTreeElement<T> WrapWithContext(AbstractTreeElement<T> element, QueryContext context) {
        if(context == null) { return element; }
        if(element instanceof QuerySensitiveTreeElement) {
            return new QuerySensitiveTreeElementWrapper<>((QuerySensitiveTreeElement<T>)element, context);
        } else {
            return element;
        }
    }

    public QuerySensitiveTreeElementWrapper(QuerySensitiveTreeElement<T> wrapped, QueryContext context) {
        this.wrapped = wrapped;
        this.context = context;
    }

    @Override
    public boolean isLeaf() {
        return wrapped.isLeaf();
    }

    @Override
    public boolean isChildable() {
        return wrapped.isChildable();
    }

    @Override
    public String getInstanceName() {
        return wrapped.getInstanceName();
    }

    @Override
    public T getChild(String name, int multiplicity) {
        return wrapped.getChild(context, name, multiplicity);
    }

    @Override
    public Vector<T> getChildrenWithName(String name) {
        return wrapped.getChildrenWithName(name);
    }

    @Override
    public boolean hasChildren() {
        return wrapped.hasChildren(context);
    }

    @Override
    public int getNumChildren() {
        return wrapped.getNumChildren();
    }

    @Override
    public T getChildAt(int i) {
        return wrapped.getChildAt(i);
    }

    @Override
    public boolean isRepeatable() {
        return wrapped.isRepeatable();
    }

    @Override
    public boolean isAttribute() {
        return wrapped.isAttribute();
    }

    @Override
    public int getChildMultiplicity(String name) {
        return wrapped.getChildMultiplicity(context, name);
    }

    @Override
    public void accept(ITreeVisitor visitor) {
        wrapped.accept(visitor);
    }

    @Override
    public int getAttributeCount() {
        return wrapped.getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return wrapped.getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        return wrapped.getAttributeName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return wrapped.getAttributeValue(index);
    }

    @Override
    public T getAttribute(String namespace, String name) {
        return wrapped.getAttribute(context, namespace, name);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return wrapped.getAttributeValue(namespace, name);
    }

    @Override
    public TreeReference getRef() {
        return wrapped.getRef(context);
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public int getMult() {
        return wrapped.getMult();
    }

    @Override
    public AbstractTreeElement getParent() {
        return wrapped.getParent();
    }

    @Override
    public IAnswerData getValue() {
        return wrapped.getValue();
    }

    @Override
    public int getDataType() {
        return wrapped.getDataType();
    }

    @Override
    public boolean isRelevant() {
        return wrapped.isRelevant();
    }

    @Override
    public String getNamespace() {
        return wrapped.getNamespace();
    }

    @Override
    public Collection<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return wrapped.tryBatchChildFetch(name, mult, predicates, evalContext);
    }
}

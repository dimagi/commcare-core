package org.commcare.cases.instance;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class StorageBackedChildElement<Model extends Externalizable>
        implements AbstractTreeElement<TreeElement> {

    protected final StorageInstanceTreeElement<Model, ?> parent;
    private TreeReference ref;
    private int numChildren = -1;
    protected int mult;

    protected StorageBackedChildElement(StorageInstanceTreeElement<Model, ?> parent,
                                        int mult) {
        this.parent = parent;
        this.mult = mult;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isChildable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getInstanceName() {
        return parent.getInstanceName();
    }

    @Override
    public TreeElement getChild(String name, int multiplicity) {
        TreeElement cached = cache();
        TreeElement child = cached.getChild(name, multiplicity);
        if (multiplicity >= 0 && child == null) {
            TreeElement emptyNode = new TreeElement(name);
            cached.addChild(emptyNode);
            emptyNode.setParent(cached);
            return emptyNode;
        }
        return child;
    }

    @Override
    public Vector<TreeElement> getChildrenWithName(String name) {
        //In order
        TreeElement cached = cache();
        Vector<TreeElement> children = cached.getChildrenWithName(name);
        if (children.size() == 0) {
            TreeElement emptyNode = new TreeElement(name);
            cached.addChild(emptyNode);
            emptyNode.setParent(cached);
            children.addElement(emptyNode);
        }
        return children;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public int getNumChildren() {
        if (numChildren == -1) {
            numChildren = cache().getNumChildren();
        }
        return numChildren;
    }

    @Override
    public TreeElement getChildAt(int i) {
        return cache().getChildAt(i);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isAttribute() {
        return false;
    }

    @Override
    public int getChildMultiplicity(String name) {
        return cache().getChildMultiplicity(name);
    }

    @Override
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getAttributeCount() {
        //TODO: Attributes should be fixed and possibly only include meta-details
        return cache().getAttributeCount();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return cache().getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        return cache().getAttributeName(index);

    }

    @Override
    public String getAttributeValue(int index) {
        return cache().getAttributeValue(index);
    }

    @Override
    public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        //TODO: We should be able to catch the index case here?
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public boolean isRelevant() {
        return true;
    }

    @Override
    public int getMult() {
        // TODO Auto-generated method stub
        return mult;
    }

    @Override
    public AbstractTreeElement getParent() {
        return parent;
    }

    @Override
    public IAnswerData getValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDataType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public TreeReference getRef() {
        if (ref == null) {
            ref = TreeReference.buildRefFromTreeElement(this);
        }
        return ref;
    }

    protected abstract TreeElement cache();
}

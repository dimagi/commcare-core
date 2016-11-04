package org.javarosa.core.model.instance;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Vector;

/**
 * @author ctsims
 */
public class InstanceBase implements AbstractTreeElement<AbstractTreeElement> {

    final String instanceName;
    AbstractTreeElement child;

    public InstanceBase(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setChild(AbstractTreeElement child) {
        this.child = child;
    }

    @Override
    public boolean isLeaf() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isChildable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public boolean hasChildren() {
        return getNumChildren() > 0;
    }

    @Override
    public AbstractTreeElement getChild(String name, int multiplicity) {
        if (name.equals(child.getName()) && multiplicity == 0) {
            return child;
        }
        return null;
    }

    @Override
    public Vector<AbstractTreeElement> getChildrenWithName(String name) {
        Vector<AbstractTreeElement> children = new Vector<>();
        if (name.equals(child.getName())) {
            children.addElement(child);
        }
        return children;
    }

    @Override
    public int getNumChildren() {
        return 1;
    }

    @Override
    public AbstractTreeElement getChildAt(int i) {
        if (i == 0) {
            return child;
        }
        return null;
    }

    @Override
    public boolean isRepeatable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAttribute() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getChildMultiplicity(String name) {
        if (name.equals(child.getName())) {
            return 1;
        }
        return 0;
    }

    @Override
    public void accept(ITreeVisitor visitor) {
        child.accept(visitor);
    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAttributeName(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAttributeValue(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AbstractTreeElement getAttribute(String namespace, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TreeReference getRef() {
        return TreeReference.rootRef();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getMult() {
        return TreeReference.DEFAULT_MUTLIPLICITY;
    }

    @Override
    public AbstractTreeElement getParent() {
        return null;
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
    public boolean isRelevant() {
        return true;
    }

    @Override
    public Vector<TreeReference> tryBatchChildFetch(String name, int mult,
                                                    Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return null;
    }

    @Override
    public String getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

}

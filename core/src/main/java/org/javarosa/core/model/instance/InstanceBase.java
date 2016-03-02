/**
 *
 */
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

    public boolean isLeaf() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isChildable() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public boolean hasChildren() {
        return getNumChildren() > 0;
    }

    public AbstractTreeElement getChild(String name, int multiplicity) {
        if (name.equals(child.getName()) && multiplicity == 0) {
            return child;
        }
        return null;
    }

    public Vector<AbstractTreeElement> getChildrenWithName(String name) {
        Vector<AbstractTreeElement> children = new Vector<AbstractTreeElement>();
        if (name.equals(child.getName())) {
            children.addElement(child);
        }
        return children;
    }

    public int getNumChildren() {
        return 1;
    }

    public AbstractTreeElement getChildAt(int i) {
        if (i == 0) {
            return child;
        }
        return null;
    }

    public boolean isRepeatable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isAttribute() {
        // TODO Auto-generated method stub
        return false;
    }

    public int getChildMultiplicity(String name) {
        if (name.equals(child.getName())) {
            return 1;
        }
        return 0;
    }

    public void accept(ITreeVisitor visitor) {
        child.accept(visitor);
    }

    public int getAttributeCount() {
        return 0;
    }

    public String getAttributeNamespace(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAttributeName(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAttributeValue(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    public AbstractTreeElement getAttribute(String namespace, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAttributeValue(String namespace, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public TreeReference getRef() {
        return TreeReference.rootRef();
    }

    public String getName() {
        return null;
    }

    public int getMult() {
        return TreeReference.DEFAULT_MUTLIPLICITY;
    }

    public AbstractTreeElement getParent() {
        return null;
    }

    public IAnswerData getValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getDataType() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean isRelevant() {
        return true;
    }

    public Vector<TreeReference> tryBatchChildFetch(String name, int mult,
                                                    Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return null;
    }

    public String getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

}

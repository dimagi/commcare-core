package org.javarosa.core.model.instance;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Enumeration;
import java.util.Vector;

/**
 * An element of a FormInstance.
 *
 * TreeElements represent an XML node in the instance. It may either have a value (e.g., <name>Drew</name>),
 * a number of TreeElement children (e.g., <meta><device /><timestamp /><user_id /></meta>), or neither (e.g.,
 * <empty_node />)
 *
 * TreeElements can also represent attributes. Attributes are unique from normal elements in that they are
 * not "children" of their parent, and are always leaf nodes: IE cannot have children.
 *
 * TODO: Split out the bind-able session data from this class and leave only the mandatory values to speed up
 * new DOM-like models
 *
 * @author Clayton Sims
 */

public class ConcreteTreeElement<T extends AbstractTreeElement> implements AbstractTreeElement<T> {
    protected String name; // can be null only for hidden root node
    protected int multiplicity = -1; // see TreeReference for special values
    protected AbstractTreeElement parent;


    protected IAnswerData value;

    //I made all of these null again because there are so many treeelements that they
    //take up a huuuge amount of space together.
    private Vector observers = null;
    protected Vector<T> attributes = null;
    protected Vector<T> children = null;

    /* model properties */
    protected int dataType = Constants.DATATYPE_NULL; //TODO

    protected String namespace;

    private String instanceName = null;

    /**
     * TreeElement with null name and 0 multiplicity? (a "hidden root" node?)
     */
    public ConcreteTreeElement() {
        this(null, TreeReference.DEFAULT_MUTLIPLICITY);
    }

    public ConcreteTreeElement(String name) {
        this(name, TreeReference.DEFAULT_MUTLIPLICITY);
    }

    public ConcreteTreeElement(String name, int multiplicity) {
        this.name = name;
        this.multiplicity = multiplicity;
        this.parent = null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
     */
    public boolean isLeaf() {
        return (children == null || children.size() == 0);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isChildable()
     */
    public boolean isChildable() {
        return (value == null);
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getInstanceName()
     */
    public String getInstanceName() {
        //CTS: I think this is a better way to do this, although I really, really don't like the duplicated code
        if (parent != null) {
            return parent.getInstanceName();
        }
        return instanceName;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setInstanceName(java.lang.String)
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setValue(org.javarosa.core.model.data.IAnswerData)
     */
    public void setValue(IAnswerData value) {
        if (isLeaf()) {
            this.value = value;
        } else {
            throw new RuntimeException("Can't set data value for node that has children!");
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
     */
    public T getChild(String name, int multiplicity) {
        if (this.children == null) {
            return null;
        }

        if (name.equals(TreeReference.NAME_WILDCARD)) {
            if (multiplicity == TreeReference.INDEX_TEMPLATE || this.children.size() < multiplicity + 1) {
                return null;
            }
            return this.children.elementAt(multiplicity); //droos: i'm suspicious of this
        } else {
            for (int i = 0; i < this.children.size(); i++) {
                T child = this.children.elementAt(i);
                if (name.equals(child.getName()) && child.getMult() == multiplicity) {
                    return child;
                }
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
     */
    public Vector<T> getChildrenWithName(String name) {
        return getChildrenWithName(name, false);
    }

    private Vector<T> getChildrenWithName(String name, boolean includeTemplate) {
        Vector<T> v = new Vector<T>();
        if (children == null) {
            return v;
        }

        for (int i = 0; i < this.children.size(); i++) {
            T child = this.children.elementAt(i);
            if ((child.getName().equals(name) || name.equals(TreeReference.NAME_WILDCARD))
                    && (includeTemplate || child.getMult() != TreeReference.INDEX_TEMPLATE))
                v.addElement(child);
        }

        return v;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
     */
    public int getNumChildren() {
        return children == null ? 0 : this.children.size();
    }

    public boolean hasChildren() {
        if (getNumChildren() > 0) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
     */
    public T getChildAt(int i) {
        return children.elementAt(i);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setDataType(int)
     */
    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#addChild(org.javarosa.core.model.instance.TreeElement)
     */
    public void addChild(T child) {
        addChild(child, false);
    }

    private void addChild(T child, boolean checkDuplicate) {
        if (!isChildable()) {
            throw new RuntimeException("Can't add children to node that has data value!");
        }

        if (child.getMult() == TreeReference.INDEX_UNBOUND) {
            throw new RuntimeException("Cannot add child with an unbound index!");
        }

        if (checkDuplicate) {
            T existingChild = getChild(child.getName(), child.getMult());
            if (existingChild != null) {
                throw new RuntimeException("Attempted to add duplicate child!");
            }
        }
        if (children == null) {
            children = new Vector();
        }

        // try to keep things in order
        int i = children.size();
        if (child.getMult() == TreeReference.INDEX_TEMPLATE) {
            T anchor = getChild(child.getName(), 0);
            if (anchor != null)
                i = children.indexOf(anchor);
        } else {
            T anchor = getChild(child.getName(),
                    (child.getMult() == 0 ? TreeReference.INDEX_TEMPLATE : child.getMult() - 1));
            if (anchor != null)
                i = children.indexOf(anchor) + 1;
        }
        children.insertElementAt(child, i);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChild(org.javarosa.core.model.instance.TreeElement)
     */
    public void removeChild(T child) {
        if (children == null) {
            return;
        }
        children.removeElement(child);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChild(java.lang.String, int)
     */
    public void removeChild(String name, int multiplicity) {
        T child = getChild(name, multiplicity);
        if (child != null) {
            removeChild(child);
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChildren(java.lang.String)
     */
    public void removeChildren(String name) {
        removeChildren(name, false);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChildren(java.lang.String, boolean)
     */
    public void removeChildren(String name, boolean includeTemplate) {
        Vector<T> v = getChildrenWithName(name, includeTemplate);
        for (int i = 0; i < v.size(); i++) {
            removeChild(v.elementAt(i));
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChildAt(int)
     */
    public void removeChildAt(int i) {
        children.removeElementAt(i);

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
     */
    public int getChildMultiplicity(String name) {
        return getChildrenWithName(name, false).size();
    }

    /* ==== MODEL PROPERTIES ==== */

    /* ==== SPECIAL SETTERS (SETTERS WITH SIDE-EFFECTS) ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setAnswer(org.javarosa.core.model.data.IAnswerData)
     */
    public boolean setAnswer(IAnswerData answer) {
        if (value != null || answer != null) {
            setValue(answer);
            return true;
        } else {
            return false;
        }
    }

    /* ==== VISITOR PATTERN ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#accept(org.javarosa.core.model.instance.utils.ITreeVisitor)
     */
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);

        if (children == null) {
            return;
        }
        Enumeration en = children.elements();
        while (en.hasMoreElements()) {
            ((ConcreteTreeElement)en.nextElement()).accept(visitor);
        }

    }

    /* ==== Attributes ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeCount()
     */
    public int getAttributeCount() {
        return attributes == null ? 0 : attributes.size();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
     */
    public String getAttributeNamespace(int index) {
        return attributes.elementAt(index).getNamespace();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
     */
    public String getAttributeName(int index) {
        return attributes.elementAt(index).getName();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        return getAttributeValue(attributes.elementAt(index));
    }

    /**
     * Get the String value of the provided attribute
     */
    private String getAttributeValue(T attribute) {
        if (attribute.getValue() == null) {
            return null;
        } else {
            return attribute.getValue().uncast().getString();
        }
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
     */
    public T getAttribute(String namespace, String name) {
        if (attributes == null) {
            return null;
        }
        for (T attribute : attributes) {
            if (attribute.getName().equals(name) && (namespace == null || namespace.equals(attribute.getNamespace()))) {
                return attribute;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespace, String name) {
        T element = getAttribute(namespace, name);
        return element == null ? null : getAttributeValue(element);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setAttribute(java.lang.String, java.lang.String, java.lang.String)
     */
    public void setAttribute(String namespace, String name, String value) {
        if (attributes == null) {
            this.attributes = new Vector<T>();
        }
        for (int i = attributes.size() - 1; i >= 0; i--) {
            T attribut = attributes.elementAt(i);
            if (attribut.getName().equals(name) && (namespace == null || namespace.equals(attribut.getNamespace()))) {
                if (value == null) {
                    attributes.removeElementAt(i);
                } else {
                    attributes.removeElementAt(i);
                    TreeElement attr = TreeElement.constructAttributeElement(namespace, name);
                    attr.setValue(new UncastData(value));
                    attr.setParent(this);
                }
                return;
            }
        }

        if (namespace == null) {
            namespace = "";
        }

        TreeElement attr = TreeElement.constructAttributeElement(namespace, name);
        attr.setValue(new UncastData(value));
        attr.setParent(this);

        attributes.addElement((T)attr);
    }

    //return the tree reference that corresponds to this tree element
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
     */
    TreeReference refCache;

    public TreeReference getRef() {
        //TODO: Expire cache somehow;
        if (refCache == null) {
            refCache = ConcreteTreeElement.BuildRef(this);
        }
        return refCache;
    }

    public static TreeReference BuildRef(AbstractTreeElement elem) {
        TreeReference ref = TreeReference.selfRef();

        while (elem != null) {
            TreeReference step;

            if (elem.getName() != null) {
                step = TreeReference.selfRef();
                step.add(elem.getName(), elem.getMult());
                step.setInstanceName(elem.getInstanceName());
            } else {
                step = TreeReference.rootRef();
                //All TreeElements are part of a consistent tree, so the root should be in the same instance
                step.setInstanceName(elem.getInstanceName());
            }

            ref = ref.parent(step);
            elem = elem.getParent();
        }
        return ref;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDepth()
     */
    public int getDepth() {
        return TreeElement.CalculateDepth(this);
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
     */
    public int getMult() {
        return multiplicity;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setMult(int)
     */
    public void setMult(int multiplicity) {
        this.multiplicity = multiplicity;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setParent(org.javarosa.core.model.instance.TreeElement)
     */
    public void setParent(AbstractTreeElement parent) {
        this.parent = parent;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getParent()
     */
    public AbstractTreeElement getParent() {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getValue()
     */
    public IAnswerData getValue() {
        return value;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#toString()
     */
    public String toString() {
        String name = "NULL";
        if (this.name != null) {
            name = this.name;
        }

        String childrenCount = "-1";
        if (this.children != null) {
            childrenCount = Integer.toString(this.children.size());
        }

        return name + " - Children: " + childrenCount;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDataType()
     */
    public int getDataType() {
        return dataType;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void clearCaches() {

    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Vector<TreeReference> tryBatchChildFetch(String name, int mult,
                                                    Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return null;
    }

    public boolean isRepeatable() {
        return true;
    }

    public boolean isAttribute() {
        return false;
    }

    public boolean isRelevant() {
        return true;
    }

}
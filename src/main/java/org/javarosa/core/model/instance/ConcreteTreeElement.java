package org.javarosa.core.model.instance;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
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

    @Override
    public boolean isLeaf() {
        return (children == null || children.size() == 0);
    }

    @Override
    public boolean isChildable() {
        return (value == null);
    }


    @Override
    public String getInstanceName() {
        //CTS: I think this is a better way to do this, although I really, really don't like the duplicated code
        if (parent != null) {
            return parent.getInstanceName();
        }
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setValue(IAnswerData value) {
        if (isLeaf()) {
            this.value = value;
        } else {
            throw new RuntimeException("Can't set data value for node that has children!");
        }
    }

    @Override
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

    @Override
    public Vector<T> getChildrenWithName(String name) {
        return getChildrenWithName(name, false);
    }

    private Vector<T> getChildrenWithName(String name, boolean includeTemplate) {
        Vector<T> v = new Vector<>();
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

    @Override
    public int getNumChildren() {
        return children == null ? 0 : this.children.size();
    }

    @Override
    public boolean hasChildren() {
        return getNumChildren() > 0;
    }

    @Override
    public T getChildAt(int i) {
        return children.elementAt(i);
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

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

    public void removeChild(T child) {
        if (children == null) {
            return;
        }
        children.removeElement(child);
    }

    public void removeChildAt(int i) {
        children.removeElementAt(i);

    }

    @Override
    public int getChildMultiplicity(String name) {
        return getChildrenWithName(name, false).size();
    }

    /* ==== MODEL PROPERTIES ==== */

    /* ==== SPECIAL SETTERS (SETTERS WITH SIDE-EFFECTS) ==== */

    public boolean setAnswer(IAnswerData answer) {
        if (value != null || answer != null) {
            setValue(answer);
            return true;
        } else {
            return false;
        }
    }

    /* ==== VISITOR PATTERN ==== */

    @Override
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

    @Override
    public int getAttributeCount() {
        return attributes == null ? 0 : attributes.size();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return attributes.elementAt(index).getNamespace();
    }

    @Override
    public String getAttributeName(int index) {
        return attributes.elementAt(index).getName();
    }

    @Override
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

    @Override
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

    @Override
    public String getAttributeValue(String namespace, String name) {
        T element = getAttribute(namespace, name);
        return element == null ? null : getAttributeValue(element);
    }

    public void setAttribute(String namespace, String name, String value) {
        if (attributes == null) {
            this.attributes = new Vector<>();
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
    TreeReference refCache;

    @Override
    public TreeReference getRef() {
        //TODO: Expire cache somehow;
        if (refCache == null) {
            refCache = TreeReference.buildRefFromTreeElement(this);
        }
        return refCache;
    }

    @Override
    public void clearVolatiles() {
        refCache = null;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int getMult() {
        return multiplicity;
    }

    public void setMult(int multiplicity) {
        this.multiplicity = multiplicity;
    }

    public void setParent(AbstractTreeElement parent) {
        this.parent = parent;
    }

    @Override
    public AbstractTreeElement getParent() {
        return parent;
    }

    @Override
    public IAnswerData getValue() {
        return value;
    }

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

    @Override
    public int getDataType() {
        return dataType;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public Collection<TreeReference> tryBatchChildFetch(String name, int mult,
                                                        Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return null;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public boolean isAttribute() {
        return false;
    }

    @Override
    public boolean isRelevant() {
        return true;
    }

}

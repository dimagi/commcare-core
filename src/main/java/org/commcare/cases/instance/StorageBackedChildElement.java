package org.commcare.cases.instance;

import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QuerySensitiveTreeElement;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.Vector;

/**
 * Semi-structured TreeElement for direct child of an external data instance
 * that loads its data from a database.
 *
 * For example represents 'case' in the path "instance('casedb')/casedb/case"
 *
 * @author ctsims
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class StorageBackedChildElement<Model extends Externalizable>
        implements QuerySensitiveTreeElement<TreeElement> {

    protected final StorageInstanceTreeElement<Model, ?> parent;
    private TreeReference ref;
    private int numChildren = -1;
    protected final int mult;
    protected int recordId;
    protected String entityId;
    protected final String nameId;

    protected StorageBackedChildElement(StorageInstanceTreeElement<Model, ?> parent,
                                        int mult, int recordId, String entityId,
                                        String nameId) {
        if (recordId == -1 && entityId == null) {
            throw new RuntimeException("Cannot create a lazy case element with no lookup identifiers!");
        }

        this.parent = parent;
        this.mult = mult;
        this.recordId = recordId;
        this.entityId = entityId;
        this.nameId = nameId;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isChildable() {
        return false;
    }

    @Override
    public String getInstanceName() {
        return parent.getInstanceName();
    }

    @Override
    public TreeElement getChild(QueryContext context, String name, int multiplicity) {
        TreeElement cached = cache(context);
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
    public TreeElement getChild(String name, int multiplicity) {
        return getChild(null, name, multiplicity);
    }

    @Override
    public Vector<TreeElement> getChildrenWithName(String name) {
        return cache().getChildrenWithName(name);
    }

    @Override
    public boolean hasChildren() {
        return hasChildren(null);
    }

    public boolean hasChildren(QueryContext context) {
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
        return getChildMultiplicity(null, name);
    }

    public int getChildMultiplicity(QueryContext context, String name) {
        return cache(context).getChildMultiplicity(name);
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
    public Collection<TreeReference> tryBatchChildFetch(String name, int mult,
                                                        Vector<XPathExpression> predicates,
                                                        EvaluationContext evalContext) {
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
        return mult;
    }

    @Override
    public AbstractTreeElement getParent() {
        return parent;
    }

    @Override
    public IAnswerData getValue() {
        return null;
    }

    @Override
    public int getDataType() {
        return 0;
    }

    @Override
    public TreeReference getRef() {
        return getRef(null);
    }


    @Override
    public TreeReference getRef(QueryContext context) {
        if (ref == null) {
            ref = TreeReference.buildRefFromTreeElement(this);
        }
        return ref;
    }


    //Context Sensitive Methods
    public TreeElement getAttribute(QueryContext context, String namespace, String name) {
        if (name.equals(nameId)) {
            if (recordId != TreeReference.INDEX_TEMPLATE) {
                //if we're already cached, don't bother with this nonsense
                synchronized (parent.treeCache) {
                    TreeElement element = parent.treeCache.retrieve(recordId);
                    if (element != null) {
                        return cache(context).getAttribute(namespace, name);
                    }
                }
            }

            //TODO: CACHE GET ID THING
            if (entityId == null) {
                return cache(context).getAttribute(namespace, name);
            }

            //otherwise, don't cache this just yet if we have the ID handy
            TreeElement entity = TreeElement.constructAttributeElement(null, name);
            entity.setValue(new StringData(entityId));
            entity.setParent(this);
            return entity;
        }
        return cache(context).getAttribute(namespace, name);
    }

    @Override
    public TreeElement getAttribute(String namespace, String name) {
        return this.getAttribute(null, namespace, name);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        if (name.equals(nameId)) {
            return entityId;
        }
        return cache().getAttributeValue(namespace, name);
    }

    protected TreeElement cache() {
        return cache(null);
    }

    protected abstract TreeElement cache(QueryContext context);


}

/**
 *
 */
package org.commcare.cases.ledger.instance;

import org.commcare.cases.ledger.Ledger;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.model.instance.utils.TreeUtilities;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 */
public class LedgerChildElement implements AbstractTreeElement<TreeElement> {

    public static final String NAME = "ledger";
    public static final String NAME_ID = "entity-id";
    public static final String SUBNAME = "section";
    public static final String SUBNAME_ID = "section-id";
    public static final String FINALNAME = "entry";
    public static final String FINALNAME_ID = "id";

    LedgerInstanceTreeElement parent;
    int recordId;
    String entityId;
    int mult;

    TreeElement empty;

    int numChildren = -1;

    public LedgerChildElement(LedgerInstanceTreeElement parent, int recordId, String entityId, int mult) {
        if (recordId == -1 && entityId == null) {
            throw new RuntimeException("Cannot create a lazy case element with no lookup identifiers!");
        }
        this.parent = parent;
        this.recordId = recordId;
        this.entityId = entityId;
        this.mult = mult;
    }

    /*
     * Template constructor (For elements that need to create reference nodesets but never look up values)
     */
    private LedgerChildElement(LedgerInstanceTreeElement parent) {
        //Template
        this.parent = parent;
        this.recordId = TreeReference.INDEX_TEMPLATE;
        this.mult = TreeReference.INDEX_TEMPLATE;
        this.entityId = null;

        empty = new TreeElement();
        empty = new TreeElement(NAME);
        empty.setMult(this.mult);

        empty.setAttribute(null, NAME_ID, "");

        TreeElement blankLedger = new TreeElement(SUBNAME);
        blankLedger.setAttribute(null, SUBNAME_ID, "");

        TreeElement scratch = new TreeElement(FINALNAME);
        scratch.setAttribute(null, FINALNAME_ID, "");
        scratch.setAnswer(null);

        blankLedger.addChild(scratch);
        empty.addChild(blankLedger);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
     */
    public boolean isLeaf() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isChildable()
     */
    public boolean isChildable() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getInstanceName()
     */
    public String getInstanceName() {
        return parent.getInstanceName();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
     */
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

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
     */
    public Vector getChildrenWithName(String name) {
        return cache().getChildrenWithName(name);
    }

    public boolean hasChildren() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
     */
    public int getNumChildren() {
        if (numChildren == -1) {
            numChildren = cache().getNumChildren();
        }
        return numChildren;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
     */
    public TreeElement getChildAt(int i) {
        return cache().getChildAt(i);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRepeatable()
     */
    public boolean isRepeatable() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isAttribute()
     */
    public boolean isAttribute() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
     */
    public int getChildMultiplicity(String name) {
        return cache().getChildMultiplicity(name);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#accept(org.javarosa.core.model.instance.utils.ITreeVisitor)
     */
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeCount()
     */
    public int getAttributeCount() {
        //TODO: Attributes should be fixed and possibly only include meta-details
        return cache().getAttributeCount();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
     */
    public String getAttributeNamespace(int index) {
        return cache().getAttributeNamespace(index);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
     */
    public String getAttributeName(int index) {
        return cache().getAttributeName(index);

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        return cache().getAttributeValue(index);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
     */
    public TreeElement getAttribute(String namespace, String name) {
        if (name.equals(NAME_ID)) {
            if (recordId != TreeReference.INDEX_TEMPLATE) {
                //if we're already cached, don't bother with this nonsense
                synchronized (parent.treeCache) {
                    TreeElement element = parent.treeCache.retrieve(recordId);
                    if (element != null) {
                        return cache().getAttribute(namespace, name);
                    }
                }
            }

            //TODO: CACHE GET ID THING
            if (entityId == null) {
                return cache().getAttribute(namespace, name);
            }

            //otherwise, don't cache this just yet if we have the ID handy
            TreeElement caseid = TreeElement.constructAttributeElement(null, name);
            caseid.setValue(new StringData(entityId));
            caseid.setParent(this);
            return caseid;
        }
        return cache().getAttribute(namespace, name);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespace, String name) {
        if (name.equals(NAME_ID)) {
            return entityId;
        }
        return cache().getAttributeValue(namespace, name);
    }

    TreeReference ref;

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
     */
    public TreeReference getRef() {
        if (ref == null) {
            ref = TreeElement.buildRef(this);
        }
        return ref;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getName()
     */
    public String getName() {
        return NAME;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
     */
    public int getMult() {
        // TODO Auto-generated method stub
        return mult;
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
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDataType()
     */
    public int getDataType() {
        // TODO Auto-generated method stub
        return 0;
    }

    //TODO: THIS IS NOT THREAD SAFE
    private TreeElement cache() {
        if (recordId == TreeReference.INDEX_TEMPLATE) {
            return empty;
        }
        synchronized (parent.treeCache) {
            TreeElement element = parent.treeCache.retrieve(recordId);
            if (element != null) {
                return element;
            }

            TreeElement cacheBuilder = new TreeElement(NAME);
            Ledger ledger = parent.storage.read(recordId);
            entityId = ledger.getEntiyId();
            cacheBuilder.setMult(this.mult);

            cacheBuilder.setAttribute(null, NAME_ID, ledger.getEntiyId());

            TreeElement ledgerElement;

            childAttributeHintMap = new Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>>();
            Hashtable<String, TreeElement[]> sectionIdMap = new Hashtable<String, TreeElement[]>();

            String[] sectionList = ledger.getSectionList();
            for (int i = 0; i < sectionList.length; ++i) {
                ledgerElement = new TreeElement(SUBNAME, i);
                ledgerElement.setAttribute(null, SUBNAME_ID, sectionList[i]);
                cacheBuilder.addChild(ledgerElement);
                sectionIdMap.put(sectionList[i], new TreeElement[]{ledgerElement});

                TreeElement entry;
                Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> hintMap = new Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>>();
                Hashtable<String, TreeElement[]> idMap = new Hashtable<String, TreeElement[]>();

                String[] entryList = ledger.getListOfEntries(sectionList[i]);
                for (int j = 0; j < entryList.length; ++j) {
                    entry = new TreeElement(FINALNAME, j);
                    entry.setAttribute(null, FINALNAME_ID, entryList[j]);
                    entry.setValue(new IntegerData(ledger.getEntry(sectionList[i], entryList[j])));
                    ledgerElement.addChild(entry);
                    idMap.put(entryList[j], new TreeElement[]{entry});
                }

                hintMap.put(TreeUtilities.getXPathAttrExpression(FINALNAME_ID), idMap);
                ledgerElement.addAttributeMap(hintMap);
            }
            childAttributeHintMap.put(TreeUtilities.getXPathAttrExpression(SUBNAME_ID), sectionIdMap);
            cacheBuilder.addAttributeMap(childAttributeHintMap);

            cacheBuilder.setParent(this.parent);

            parent.treeCache.register(recordId, cacheBuilder);

            return cacheBuilder;
        }
    }

    public boolean isRelevant() {
        return true;
    }

    public static LedgerChildElement TemplateElement(LedgerInstanceTreeElement parent) {
        LedgerChildElement template = new LedgerChildElement(parent);
        return template;
    }

    Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> childAttributeHintMap = null;

    public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return TreeUtilities.tryBatchChildFetch(this, childAttributeHintMap, name, mult, predicates, evalContext);
    }

    public String getNamespace() {
        return null;
    }

}

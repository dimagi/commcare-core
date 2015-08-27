/**
 *
 */
package org.commcare.cases.ledger.instance;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.util.StorageBackedTreeRoot;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.Interner;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;
import java.util.Vector;

/**
 * The root element for the <casedb> abstract type. All children are
 * nodes in the case database. Depending on instantiation, the <casedb>
 * may include only a subset of the full db.
 *
 * @author ctsims
 */
public class LedgerInstanceTreeElement extends StorageBackedTreeRoot<LedgerChildElement> {

    public static final String MODEL_NAME = "ledgerdb";

    private AbstractTreeElement instanceRoot;

    IStorageUtilityIndexed<Ledger> storage;
    private String[] ledgerRecords;

    //TODO: much of this is still shared w/the casedb and should be centralized there
    protected Vector<LedgerChildElement> ledgers;

    protected Interner<TreeElement> treeCache = new Interner<TreeElement>();

    protected Interner<String> stringCache;

    public LedgerInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed storage) {
        this.instanceRoot = instanceRoot;
        this.storage = storage;
        storage.setReadOnly();
    }

    /**
     * Rebase assigns this tree element to a new root instance node.
     *
     * Used to migrate the already created tree structure to a new instance connector.
     *
     * @param instanceRoot The root of the new tree that this element should be a part of
     */
    public void rebase(AbstractTreeElement instanceRoot) {
        this.instanceRoot = instanceRoot;
        expireCachedRef();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
     */
    public boolean isLeaf() {
        // TODO Auto-generated method stub
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
        return instanceRoot.getInstanceName();
    }

    public void attachStringCache(Interner<String> stringCache) {
        this.stringCache = stringCache;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
     */
    public LedgerChildElement getChild(String name, int multiplicity) {
        if ((multiplicity == TreeReference.INDEX_TEMPLATE) &&
                "ledger".equals(name)) {
            return null;
            // TODO PLM: commenting out the following line until http://manage.dimagi.com/default.asp?179998 is fixed
            // this will break the ledger query silent fail fix for http://manage.dimagi.com/default.asp?177247
            // return LedgerChildElement.TemplateElement(this);
        }

        //name is always the same, so multiplicities are the only relevant component here
        if (name.equals(LedgerChildElement.NAME)) {
            getLedgers();
            if (ledgers.size() == 0) {
                //If we have no ledgers, we still need to be able to return a template element so as to not
                //break xpath evaluation
                return LedgerChildElement.TemplateElement(this);
            }
            return ledgers.elementAt(multiplicity);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
     */
    public Vector getChildrenWithName(String name) {
        if (name.equals(LedgerChildElement.NAME)) {
            getLedgers();
            return ledgers;
        } else {
            return new Vector();
        }

    }

    int numRecords = -1;

    public boolean hasChildren() {
        if (getNumChildren() > 0) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
     */
    public int getNumChildren() {
        if (ledgerRecords != null) {
            return ledgerRecords.length;
        } else {
            if (numRecords == -1) {
                numRecords = storage.getNumRecords();
            }
            return numRecords;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
     */
    public LedgerChildElement getChildAt(int i) {
        getLedgers();
        return ledgers.elementAt(i);
    }

    protected synchronized void getLedgers() {
        if (ledgers != null) {
            return;
        }
        objectIdMapping = new Hashtable<Integer, Integer>();
        ledgers = new Vector<LedgerChildElement>();
        if (ledgerRecords != null) {
            int i = 0;
            for (String id : ledgerRecords) {
                ledgers.addElement(new LedgerChildElement(this, -1, id, i));
                ++i;
            }
        } else {
            int mult = 0;
            for (IStorageIterator i = storage.iterate(); i.hasMore(); ) {
                int id = i.nextID();
                ledgers.addElement(new LedgerChildElement(this, id, null, mult));
                objectIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
                mult++;
            }

        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRepeatable()
     */
    public boolean isRepeatable() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isAttribute()
     */
    public boolean isAttribute() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
     */
    public int getChildMultiplicity(String name) {
        //All children have the same name;
        if (name.equals(LedgerChildElement.NAME)) {
            return this.getNumChildren();
        } else {
            return 0;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRelevant()
     */
    public boolean isRelevant() {
        return true;
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
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
     */
    public String getAttributeNamespace(int index) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
     */
    public String getAttributeName(int index) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
     */
    public LedgerChildElement getAttribute(String namespace, String name) {
        //Oooooof, this is super janky;
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    TreeReference cachedRef = null;

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
     */
    public TreeReference getRef() {
        if (cachedRef == null) {
            cachedRef = TreeElement.BuildRef(this);
        }
        return cachedRef;
    }

    private void expireCachedRef() {
        cachedRef = null;
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
        return MODEL_NAME;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
     */
    public int getMult() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getParent()
     */
    public AbstractTreeElement getParent() {
        return instanceRoot;
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

    public void clearCaches() {
        // TODO Auto-generated method stub
    }

    public String getNamespace() {
        return null;
    }

    public String intern(String s) {
        if (stringCache == null) {
            return s;
        } else {
            return stringCache.intern(s);
        }
    }

    protected String getChildHintName() {
        return "ledger";
    }

    final static private XPathPathExpr ENTITY_ID_EXPR = XPathReference.getPathExpr("@entity-id");
    final static private XPathPathExpr ENTITY_ID_EXPR_TWO = XPathReference.getPathExpr("./@entity-id");


    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        Hashtable<XPathPathExpr, String> indices = new Hashtable<XPathPathExpr, String>();

        //TODO: Much better matching
        indices.put(ENTITY_ID_EXPR, Ledger.INDEX_ENTITY_ID);
        indices.put(ENTITY_ID_EXPR_TWO, Ledger.INDEX_ENTITY_ID);

        return indices;
    }

    protected IStorageUtilityIndexed<?> getStorage() {
        return storage;
    }

    protected void initStorageCache() {
        getLedgers();
    }
}

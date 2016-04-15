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
 * @author ctsims
 */
public class LedgerInstanceTreeElement extends StorageBackedTreeRoot<LedgerChildElement> {

    public static final String MODEL_NAME = "ledgerdb";

    private final static XPathPathExpr ENTITY_ID_EXPR = XPathReference.getPathExpr("@entity-id");
    private final static XPathPathExpr ENTITY_ID_EXPR_TWO = XPathReference.getPathExpr("./@entity-id");

    private AbstractTreeElement instanceRoot;

    final IStorageUtilityIndexed<Ledger> storage;

    //TODO: much of this is still shared w/the casedb and should be centralized there
    protected Vector<LedgerChildElement> ledgers;

    protected final Interner<TreeElement> treeCache = new Interner<TreeElement>();

    protected Interner<String> stringCache;

    int numRecords = -1;

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
        return instanceRoot.getInstanceName();
    }

    @SuppressWarnings("unused")
    public void attachStringCache(Interner<String> stringCache) {
        this.stringCache = stringCache;
    }

    @Override
    public LedgerChildElement getChild(String name, int multiplicity) {
        if ((multiplicity == TreeReference.INDEX_TEMPLATE) &&
                "ledger".equals(name)) {
            return LedgerChildElement.TemplateElement(this);
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

    @Override
    public Vector<LedgerChildElement> getChildrenWithName(String name) {
        if (name.equals(LedgerChildElement.NAME)) {
            getLedgers();
            return ledgers;
        } else {
            return new Vector<LedgerChildElement>();
        }

    }

    @Override
    public boolean hasChildren() {
        return (getNumChildren() > 0);
    }

    @Override
    public int getNumChildren() {
        if (numRecords == -1) {
            numRecords = storage.getNumRecords();
        }
        return numRecords;
    }

    @Override
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
        int mult = 0;
        for (IStorageIterator i = storage.iterate(); i.hasMore(); ) {
            int id = i.nextID();
            ledgers.addElement(new LedgerChildElement(this, id, null, mult));
            objectIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
            mult++;
        }
    }

    public boolean isRepeatable() {
        return false;
    }

    @Override
    public boolean isAttribute() {
        return false;
    }

    @Override
    public int getChildMultiplicity(String name) {
        //All children have the same name;
        if (name.equals(LedgerChildElement.NAME)) {
            return this.getNumChildren();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isRelevant() {
        return true;
    }

    @Override
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);

    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        return null;
    }

    @Override
    public String getAttributeName(int index) {
        return null;
    }

    @Override
    public String getAttributeValue(int index) {
        return null;
    }

    @Override
    public LedgerChildElement getAttribute(String namespace, String name) {
        //Oooooof, this is super janky;
        return null;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        return null;
    }

    TreeReference cachedRef = null;

    @Override
    public TreeReference getRef() {
        if (cachedRef == null) {
            cachedRef = TreeReference.buildRefFromTreeElement(this);
        }
        return cachedRef;
    }

    private void expireCachedRef() {
        cachedRef = null;
    }

    @Override
    public String getName() {
        return MODEL_NAME;
    }

    @Override
    public int getMult() {
        return 0;
    }

    @Override
    public AbstractTreeElement getParent() {
        return instanceRoot;
    }

    @Override
    public IAnswerData getValue() {
        return null;
    }

    @Override
    public int getDataType() {
        return 0;
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

    @Override
    protected String getChildHintName() {
        return "ledger";
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        Hashtable<XPathPathExpr, String> indices = new Hashtable<XPathPathExpr, String>();

        //TODO: Much better matching
        indices.put(ENTITY_ID_EXPR, Ledger.INDEX_ENTITY_ID);
        indices.put(ENTITY_ID_EXPR_TWO, Ledger.INDEX_ENTITY_ID);

        return indices;
    }

    @Override
    protected IStorageUtilityIndexed<?> getStorage() {
        return storage;
    }

    @Override
    protected void initStorageCache() {
        getLedgers();
    }
}

/**
 *
 */
package org.commcare.cases.instance;

import org.commcare.cases.model.Case;
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
public class CaseInstanceTreeElement extends StorageBackedTreeRoot<CaseChildElement> {

    public static final String MODEL_NAME = "casedb";

    private AbstractTreeElement instanceRoot;

    protected IStorageUtilityIndexed storage;
    private String[] caseRecords;

    protected Vector<CaseChildElement> cases;

    protected Interner<TreeElement> treeCache = new Interner<TreeElement>();

    protected Interner<String> stringCache;

    String syncToken;
    String stateHash;

    /**
     * In report mode, casedb is not the root of a document, and we only build the top
     * level case node (not the whole thing)
     */
    boolean reportMode;

    public CaseInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed storage, String[] caseIDs) {
        this(instanceRoot, storage, false);
        this.caseRecords = caseIDs;
    }

    public CaseInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed storage, boolean reportMode) {
        this.instanceRoot = instanceRoot;
        this.storage = storage;
        this.reportMode = reportMode;
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
    public CaseChildElement getChild(String name, int multiplicity) {
        if ((multiplicity == TreeReference.INDEX_TEMPLATE) &&
                "case".equals(name)) {
            return CaseChildElement.buildCaseChildTemplate(this);
        }

        //name is always "case", so multiplicities are the only relevant component here
        if (name.equals("case")) {
            getCases();
            if (cases.size() == 0) {
                //If we have no cases, we still need to be able to return a template element so as to not
                //break xpath evaluation
                return CaseChildElement.buildCaseChildTemplate(this);
            }
            return cases.elementAt(multiplicity);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
     */
    public Vector getChildrenWithName(String name) {
        if (name.equals("case")) {
            getCases();
            return cases;
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
        if (caseRecords != null) {
            return caseRecords.length;
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
    public CaseChildElement getChildAt(int i) {
        getCases();
        return cases.elementAt(i);
    }

    protected synchronized void getCases() {
        if (cases != null) {
            return;
        }
        objectIdMapping = new Hashtable<Integer, Integer>();
        cases = new Vector<CaseChildElement>();
        if (caseRecords != null) {
            int i = 0;
            for (String id : caseRecords) {
                cases.addElement(new CaseChildElement(this, -1, id, i));
                ++i;
            }
        } else {
            int mult = 0;
            for (IStorageIterator i = storage.iterate(); i.hasMore(); ) {
                int id = i.nextID();
                cases.addElement(new CaseChildElement(this, id, null, mult));
                objectIdMapping.put(DataUtil.integer(id), DataUtil.integer(mult));
                mult++;
            }

        }
    }

    public void setState(String syncToken, String stateHash) {
        this.syncToken = syncToken;
        this.stateHash = stateHash;
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
        if (name.equals("case")) {
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
        if (syncToken == null) {
            return 0;
        }
        return 2;
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
        if (index == 0) {
            return "syncToken";
        } else if (index == 1) {
            return "stateHash";
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        if (index == 0) {
            return syncToken;
        } else if (index == 1) {
            return stateHash;
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
     */
    public CaseChildElement getAttribute(String namespace, String name) {
        //Oooooof, this is super janky;
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespace, String name) {
        return getAttributeValue("syncToken".equals(name) ? 0 : "stateHash".equals(name) ? 1 : -1);
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

    //Xpath parsing is sllllllloooooooowwwwwww
    final static private XPathPathExpr CASE_ID_EXPR = XPathReference.getPathExpr("@case_id");
    final static private XPathPathExpr CASE_ID_EXPR_TWO = XPathReference.getPathExpr("./@case_id");
    final static private XPathPathExpr CASE_TYPE_EXPR = XPathReference.getPathExpr("@case_type");
    final static private XPathPathExpr CASE_STATUS_EXPR = XPathReference.getPathExpr("@status");
    final static private XPathPathExpr CASE_INDEX_EXPR = XPathReference.getPathExpr("index/*");


    protected String translateFilterExpr(XPathPathExpr expressionTemplate, XPathPathExpr matchingExpr, Hashtable<XPathPathExpr, String> indices) {
        String filter = super.translateFilterExpr(expressionTemplate, matchingExpr, indices);

        //If we're matching a case index, we've got some magic to take care of. First,
        //generate the expected case ID
        if (expressionTemplate == CASE_INDEX_EXPR) {
            filter += ((XPathPathExpr)matchingExpr).steps[1].name.name;
        }

        return filter;
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

    public Case getCase(int recordId) {
        return (Case)storage.read(recordId);
    }

    protected String getChildHintName() {
        return "case";
    }

    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        Hashtable<XPathPathExpr, String> indices = new Hashtable<XPathPathExpr, String>();

        //TODO: Much better matching
        indices.put(CASE_ID_EXPR, Case.INDEX_CASE_ID);
        indices.put(CASE_ID_EXPR_TWO, Case.INDEX_CASE_ID);
        indices.put(CASE_TYPE_EXPR, Case.INDEX_CASE_TYPE);
        indices.put(CASE_STATUS_EXPR, Case.INDEX_CASE_STATUS);
        indices.put(CASE_INDEX_EXPR, Case.INDEX_CASE_INDEX_PRE);

        return indices;
    }

    protected IStorageUtilityIndexed<?> getStorage() {
        return storage;
    }

    protected void initStorageCache() {
        getCases();
    }
}

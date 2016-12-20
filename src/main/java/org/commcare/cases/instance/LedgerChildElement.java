package org.commcare.cases.instance;

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

    StorageInstanceTreeElement<Ledger, ?> parent;
    int recordId;
    String entityId;
    int mult;

    TreeElement empty;

    int numChildren = -1;

    public LedgerChildElement(StorageInstanceTreeElement<Ledger, ?> parent, int recordId, String entityId, int mult) {
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
    private LedgerChildElement(StorageInstanceTreeElement<Ledger, ?> parent) {
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
    public Vector getChildrenWithName(String name) {
        return cache().getChildrenWithName(name);
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

    @Override
    public String getAttributeValue(String namespace, String name) {
        if (name.equals(NAME_ID)) {
            return entityId;
        }
        return cache().getAttributeValue(namespace, name);
    }

    TreeReference ref;

    @Override
    public TreeReference getRef() {
        if (ref == null) {
            ref = TreeReference.buildRefFromTreeElement(this);
        }
        return ref;
    }

    @Override
    public String getName() {
        return NAME;
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
            Ledger ledger = parent.getElement(recordId);
            entityId = ledger.getEntiyId();
            cacheBuilder.setMult(this.mult);

            cacheBuilder.setAttribute(null, NAME_ID, ledger.getEntiyId());

            TreeElement ledgerElement;

            childAttributeHintMap = new Hashtable<>();
            Hashtable<String, TreeElement[]> sectionIdMap = new Hashtable<>();

            String[] sectionList = ledger.getSectionList();
            for (int i = 0; i < sectionList.length; ++i) {
                ledgerElement = new TreeElement(SUBNAME, i);
                ledgerElement.setAttribute(null, SUBNAME_ID, sectionList[i]);
                cacheBuilder.addChild(ledgerElement);
                sectionIdMap.put(sectionList[i], new TreeElement[]{ledgerElement});

                TreeElement entry;
                Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> hintMap = new Hashtable<>();
                Hashtable<String, TreeElement[]> idMap = new Hashtable<>();

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

    @Override
    public boolean isRelevant() {
        return true;
    }

    public static LedgerChildElement TemplateElement(LedgerInstanceTreeElement parent) {
        return new LedgerChildElement(parent);
    }

    Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> childAttributeHintMap = null;

    @Override
    public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return TreeUtilities.tryBatchChildFetch(this, childAttributeHintMap, name, mult, predicates, evalContext);
    }

    @Override
    public String getNamespace() {
        return null;
    }

}

package org.commcare.cases.instance;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.query.QueryContext;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.TreeUtilities;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author ctsims
 */
public class LedgerChildElement extends StorageBackedChildElement<Ledger> {

    public static final String NAME = "ledger";
    private static final String NAME_ID = "entity-id";
    private static final String SUBNAME = "section";
    private static final String SUBNAME_ID = "section-id";
    private static final String FINALNAME = "entry";
    private static final String FINALNAME_ID = "id";

    private Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> childAttributeHintMap = null;

    private TreeElement empty;

    public LedgerChildElement(StorageInstanceTreeElement<Ledger, ?> parent,
                              int recordId, String entityId, int mult) {
        super(parent, mult, recordId, entityId, NAME_ID);
    }

    /*
     * Template constructor (For elements that need to create reference nodesets but never look up values)
     */
    private LedgerChildElement(StorageInstanceTreeElement<Ledger, ?> parent) {
        super(parent, TreeReference.INDEX_TEMPLATE, TreeReference.INDEX_TEMPLATE, null, NAME_ID);

        empty = new TreeElement(NAME);
        empty.setMult(this.mult);

        empty.setAttribute(null, nameId, "");

        TreeElement blankLedger = new TreeElement(SUBNAME);
        blankLedger.setAttribute(null, SUBNAME_ID, "");

        TreeElement scratch = new TreeElement(FINALNAME);
        scratch.setAttribute(null, FINALNAME_ID, "");
        scratch.setAnswer(null);

        blankLedger.addChild(scratch);
        empty.addChild(blankLedger);
    }

    @Override
    public String getName() {
        return NAME;
    }

    //TODO: THIS IS NOT THREAD SAFE
    @Override
    protected TreeElement cache(QueryContext context) {
        if (recordId == TreeReference.INDEX_TEMPLATE) {
            return empty;
        }
        synchronized (parent.treeCache) {
            TreeElement element = parent.treeCache.retrieve(recordId);
            if (element != null) {
                return element;
            }

            TreeElement cacheBuilder = new TreeElement(NAME);
            Ledger ledger = parent.getElement(recordId, context);
            entityId = ledger.getEntiyId();
            cacheBuilder.setMult(this.mult);

            cacheBuilder.setAttribute(null, nameId, ledger.getEntiyId());

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

    protected static LedgerChildElement TemplateElement(LedgerInstanceTreeElement parent) {
        return new LedgerChildElement(parent);
    }

    @Override
    public Collection<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return TreeUtilities.tryBatchChildFetch(this, childAttributeHintMap, name, mult, predicates, evalContext);
    }
}

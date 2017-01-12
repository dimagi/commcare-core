package org.commcare.cases.instance;

import org.commcare.cases.ledger.Ledger;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * @author ctsims
 */
public class LedgerInstanceTreeElement
        extends StorageInstanceTreeElement<Ledger, LedgerChildElement> {

    public static final String MODEL_NAME = "ledgerdb";

    private final static XPathPathExpr ENTITY_ID_EXPR = XPathReference.getPathExpr("@entity-id");
    private final static XPathPathExpr ENTITY_ID_EXPR_TWO = XPathReference.getPathExpr("./@entity-id");

    public LedgerInstanceTreeElement(AbstractTreeElement instanceRoot, IStorageUtilityIndexed<Ledger> storage) {
        super(instanceRoot, storage, MODEL_NAME, "ledger");
    }

    @Override
    protected LedgerChildElement buildElement(StorageInstanceTreeElement<Ledger, LedgerChildElement> storageInstance,
                                              int recordId, String id, int mult) {
        return new LedgerChildElement(storageInstance, recordId, null, mult);
    }

    @Override
    protected LedgerChildElement getChildTemplate(StorageInstanceTreeElement parent) {
        return LedgerChildElement.TemplateElement(this);
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        Hashtable<XPathPathExpr, String> indices = new Hashtable<>();

        //TODO: Much better matching
        indices.put(ENTITY_ID_EXPR, Ledger.INDEX_ENTITY_ID);
        indices.put(ENTITY_ID_EXPR_TWO, Ledger.INDEX_ENTITY_ID);

        return indices;
    }
}

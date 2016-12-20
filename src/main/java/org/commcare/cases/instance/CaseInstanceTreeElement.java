package org.commcare.cases.instance;

import org.commcare.cases.model.Case;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * The root element for the <casedb> abstract type. All children are
 * nodes in the case database. Depending on instantiation, the <casedb>
 * may include only a subset of the full db.
 *
 * @author ctsims
 */
public class CaseInstanceTreeElement extends StorageInstanceTreeElement<Case, CaseChildElement> {

    public static final String MODEL_NAME = "casedb";

    //Xpath parsing is sllllllloooooooowwwwwww
    private final static XPathPathExpr CASE_ID_EXPR = XPathReference.getPathExpr("@case_id");
    private final static XPathPathExpr CASE_ID_EXPR_TWO = XPathReference.getPathExpr("./@case_id");
    private final static XPathPathExpr CASE_TYPE_EXPR = XPathReference.getPathExpr("@case_type");
    private final static XPathPathExpr CASE_STATUS_EXPR = XPathReference.getPathExpr("@status");
    private final static XPathPathExpr CASE_INDEX_EXPR = XPathReference.getPathExpr("index/*");

    public CaseInstanceTreeElement(AbstractTreeElement instanceRoot,
                                   IStorageUtilityIndexed<Case> storage) {
        super(instanceRoot, storage, MODEL_NAME, "case");
    }

    @Override
    protected CaseChildElement buildElement(StorageInstanceTreeElement<Case, CaseChildElement> storageInstance,
                                            int recordId, String id, int mult) {
        return new CaseChildElement(storageInstance, recordId, null, mult);
    }

    @Override
    protected CaseChildElement getChildTemplate(StorageInstanceTreeElement parent) {
        return CaseChildElement.buildCaseChildTemplate(this);
    }

    @Override
    protected String translateFilterExpr(XPathPathExpr expressionTemplate, XPathPathExpr matchingExpr,
                                         Hashtable<XPathPathExpr, String> indices) {
        String filter = super.translateFilterExpr(expressionTemplate, matchingExpr, indices);

        //If we're matching a case index, we've got some magic to take care of. First,
        //generate the expected case ID
        if (expressionTemplate == CASE_INDEX_EXPR) {
            filter += matchingExpr.steps[1].name.name;
        }

        return filter;
    }

    @Override
    protected Hashtable<XPathPathExpr, String> getStorageIndexMap() {
        Hashtable<XPathPathExpr, String> indices = new Hashtable<>();

        //TODO: Much better matching
        indices.put(CASE_ID_EXPR, Case.INDEX_CASE_ID);
        indices.put(CASE_ID_EXPR_TWO, Case.INDEX_CASE_ID);
        indices.put(CASE_TYPE_EXPR, Case.INDEX_CASE_TYPE);
        indices.put(CASE_STATUS_EXPR, Case.INDEX_CASE_STATUS);
        indices.put(CASE_INDEX_EXPR, Case.INDEX_CASE_INDEX_PRE);

        return indices;
    }
}

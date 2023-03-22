package org.commcare.xml.bulk;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.modern.engine.cases.CaseIndexTable;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.SerializationLimitationException;
import org.javarosa.xml.util.ActionableInvalidStructureException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The BulkCaseInstanceXmlParser Parser is responsible for parsing case instance xml structure
 * as specified in https://github.com/dimagi/commcare-core/wiki/casedb#casedb-instance-structure
 */

// todo this and other case parsers duplicates a bunch of logic today that can be unified
public class BulkCaseInstanceXmlParser extends BulkElementParser<Case> {

    private static final String CASE_PROPERTY_CASE_ID = "case_id";
    private static final String CASE_PROPERTY_CASE_TYPE = "case_type";
    private static final String CASE_PROPERTY_OWNER_ID = "owner_id";
    private static final String CASE_PROPERTY_STATUS = "status";
    private static final String CASE_PROPERTY_CASE_NAME = "case_name";
    private static final String CASE_PROPERTY_LAST_MODIFIED = "last_modified";
    private static final String CASE_PROPERTY_DATE_OPENED = "date_opened";
    private static final String CASE_PROPERTY_EXTERNAL_ID = "external_id";
    private static final String CASE_PROPERTY_CATEGORY = "category";
    private static final String CASE_PROPERTY_STATE = "state";
    private static final String CASE_PROPERTY_INDEX = "index";

    private final CaseIndexTable mCaseIndexTable;
    private final IStorageUtilityIndexed<Case> storage;

    public BulkCaseInstanceXmlParser(KXmlParser parser, IStorageUtilityIndexed<Case> storage,
            CaseIndexTable caseIndexTable) {
        super(parser);
        this.mCaseIndexTable = caseIndexTable;
        this.storage = storage;
    }

    @Override
    protected void requestModelReadsForElement(TreeElement bufferedTreeElement, Set<String> currentBulkReadSet) {
        String caseId = bufferedTreeElement.getAttributeValue(null, "case_id");
        currentBulkReadSet.add(caseId);
    }

    @Override
    protected void preParseValidate() throws InvalidStructureException {
        checkNode("case");
    }

    @Override
    protected void processBufferedElement(TreeElement bufferedTreeElement, Map<String, Case> currentOperatingSet,
            LinkedHashMap<String, Case> writeLog) throws InvalidStructureException {
        String caseId = bufferedTreeElement.getAttributeValue(null, CASE_PROPERTY_CASE_ID);
        validateMandatoryProperty(CASE_PROPERTY_CASE_ID, caseId);

        String caseType = bufferedTreeElement.getAttributeValue(null, CASE_PROPERTY_CASE_TYPE);
        validateMandatoryProperty(CASE_PROPERTY_CASE_TYPE, caseType);

        String ownerId = bufferedTreeElement.getAttributeValue(null, CASE_PROPERTY_OWNER_ID);
        validateMandatoryProperty(CASE_PROPERTY_OWNER_ID, ownerId);

        String status = bufferedTreeElement.getAttributeValue(null, CASE_PROPERTY_STATUS);
        validateMandatoryProperty(CASE_PROPERTY_STATUS, status);

        Case caseForBlock = currentOperatingSet.get(caseId);
        if (caseForBlock == null) {
            caseForBlock = buildCase(null, caseType);
            caseForBlock.setCaseId(caseId);
        } else {
            caseForBlock.setTypeId(caseType);
        }
        caseForBlock.setUserId(ownerId);
        caseForBlock.setClosed(status.contentEquals("closed"));

        updateCase(bufferedTreeElement, caseForBlock, caseId);
        validateCase(caseForBlock);

        try {
            writeLog.put(caseForBlock.getCaseId(), caseForBlock);
            currentOperatingSet.put(caseForBlock.getCaseId(), caseForBlock);
        } catch (SerializationLimitationException e) {
            throw new InvalidStructureException("One of the property values for the case named '" +
                    caseForBlock.getName() + "' is too large (by " + e.percentOversized +
                    "%). Please show your supervisor.");
        }
    }

    private void validateCase(Case caseForBlock) throws InvalidStructureException {
        validateMandatoryProperty(CASE_PROPERTY_LAST_MODIFIED, caseForBlock.getLastModified());
        validateMandatoryProperty(CASE_PROPERTY_CASE_NAME, caseForBlock.getName());
    }

    private static String getTrimmedElementTextOrBlank(TreeElement element) {
        if (element.getValue() == null) {
            return "";
        }
        return element.getValue().uncast().getString().trim();
    }

    private static void validateMandatoryProperty(String key, Object value) throws InvalidStructureException {
        if (value == null || value.equals("")) {
            String error = String.format("The %s attribute of a <case> wasn't set", key);
            throw new InvalidStructureException(error);
        }
    }

    protected Case buildCase(String name, String typeId) {
        return new Case(name, typeId);
    }

    private void updateCase(TreeElement updateElement,
            Case caseForBlock, String caseId) throws InvalidStructureException {

        for (int i = 0; i < updateElement.getNumChildren(); i++) {
            TreeElement subElement = updateElement.getChildAt(i);

            String key = subElement.getName();
            String value = getTrimmedElementTextOrBlank(subElement);

            switch (key) {
                case CASE_PROPERTY_CASE_NAME:
                    caseForBlock.setName(value);
                    break;
                case CASE_PROPERTY_DATE_OPENED:
                    caseForBlock.setDateOpened(DateUtils.parseDate(value));
                    break;
                case CASE_PROPERTY_LAST_MODIFIED:
                    caseForBlock.setLastModified(DateUtils.parseDateTime(value));
                    break;
                case CASE_PROPERTY_EXTERNAL_ID:
                    caseForBlock.setExternalId(value);
                    break;
                case CASE_PROPERTY_CATEGORY:
                    caseForBlock.setCategory(value);
                    break;
                case CASE_PROPERTY_STATE:
                    caseForBlock.setState(value);
                    break;
                case CASE_PROPERTY_INDEX:
                    indexCase(subElement, caseForBlock, caseId);
                    break;
                default:
                    caseForBlock.setProperty(key, value);
                    break;
            }
        }
    }

    @Override
    protected void performBulkRead(Set<String> currentBulkReadSet, Map<String, Case> currentOperatingSet) {
        for (Case c : storage.getBulkRecordsForIndex(Case.INDEX_CASE_ID, currentBulkReadSet)) {
            currentOperatingSet.put(c.getCaseId(), c);
        }
    }

    @Override
    protected void performBulkWrite(LinkedHashMap<String, Case> writeLog) throws IOException {
        ArrayList<Integer> recordIdsToWipe = new ArrayList<>();
        for (String caseId : writeLog.keySet()) {
            Case c = writeLog.get(caseId);
            storage.write(c);
            // Add the case's SQL record ID
            recordIdsToWipe.add(c.getID());
        }
        if (mCaseIndexTable != null) {
            mCaseIndexTable.clearCaseIndices(recordIdsToWipe);
            for (String cid : writeLog.keySet()) {
                Case c = writeLog.get(cid);
                mCaseIndexTable.indexCase(c);
            }
        }
    }

    private static void indexCase(TreeElement indexElement, Case caseForBlock, String caseId)
            throws InvalidStructureException {
        for (int i = 0; i < indexElement.getNumChildren(); i++) {
            TreeElement subElement = indexElement.getChildAt(i);

            String indexName = subElement.getName();
            String caseType = subElement.getAttributeValue(null, "case_type");

            String relationship = subElement.getAttributeValue(null, "relationship");
            if (relationship == null) {
                relationship = CaseIndex.RELATIONSHIP_CHILD;
            }

            String value = getTrimmedElementTextOrBlank(subElement);

            if (value.equals(caseId)) {
                throw new ActionableInvalidStructureException("case.error.self.index", new String[]{caseId},
                        "Case " + caseId + " cannot index itself");
            }

            //Remove any ambiguity associated with empty values
            if (value.equals("")) {
                value = null;
            }

            if ("".equals(relationship)) {
                throw new InvalidStructureException(String.format(
                        "Invalid Case Transaction for Case[%s]: Attempt to add a '' relationship type to "
                                + "entity[%s]",
                        caseId, value));
            }


            //Process blank inputs in the same manner as data fields (IE: Remove the underlying model)
            if (value == null) {
                caseForBlock.removeIndex(indexName);
            } else {
                caseForBlock.setIndex(new CaseIndex(indexName, caseType, value,
                        relationship));
            }
        }
    }
}

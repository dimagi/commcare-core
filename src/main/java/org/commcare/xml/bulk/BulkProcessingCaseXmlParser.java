package org.commcare.xml.bulk;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.util.externalizable.SerializationLimitationException;
import org.javarosa.xml.util.ActionableInvalidStructureException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * A parser which is capable of processing CaseXML transactions in a bulk format.
 *
 * This parser needs an implementation which can perform the "bulk" steps efficiently on the current
 * platform.
 *
 * It should be a drop-in replacemenet for the CaseXmlParser in the ways that it is used
 *
 * Created by ctsims on 3/14/2017.
 */
public abstract class BulkProcessingCaseXmlParser extends BulkElementParser<Case> {

    public BulkProcessingCaseXmlParser(KXmlParser parser) {
        super(parser);
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
    protected void processBufferedElement(TreeElement bufferedTreeElement, Map<String, Case> currentOperatingSet, SortedMap<String, Case> writeLog) throws InvalidStructureException {
        String caseId = bufferedTreeElement.getAttributeValue(null, "case_id");
        if (caseId == null || caseId.equals("")) {
            throw new InvalidStructureException("The case_id attribute of a <case> wasn't set");
        }

        String dateModified = bufferedTreeElement.getAttributeValue(null, "date_modified");
        if (dateModified == null) {
            throw new InvalidStructureException("The date_modified attribute of a <case> wasn't set");
        }
        Date modified = DateUtils.parseDateTime(dateModified);

        Case caseForBlock = null;
        boolean isCreateOrUpdate = false;

        for (int i = 0; i < bufferedTreeElement.getNumChildren(); i++) {
            TreeElement subElement = bufferedTreeElement.getChildAt(i);
            String action = subElement.getName().toLowerCase();
            switch (action) {
                case "create":
                    caseForBlock = createCase(subElement, currentOperatingSet, caseId, modified);
                    isCreateOrUpdate = true;
                    break;
                case "update":
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, true);
                    updateCase(subElement, caseForBlock, caseId);
                    isCreateOrUpdate = true;
                    break;
                case "close":
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, true);
                    closeCase(caseForBlock, caseId);
                    break;
                case "index":
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, false);
                    indexCase(subElement, caseForBlock, caseId);
                    break;
                case "attachment":
                    caseForBlock = loadCase(caseForBlock, caseId, currentOperatingSet, false);
                    processCaseAttachment(subElement, caseForBlock);
                    break;
            }

        }

        if (caseForBlock != null) {
            caseForBlock.setLastModified(modified);

            try {
                writeLog.put(caseForBlock.getCaseId(), caseForBlock);
            } catch (SerializationLimitationException e) {
                throw new InvalidStructureException("One of the property values for the case named '" +
                        caseForBlock.getName() + "' is too large (by " + e.percentOversized +
                        "%). Please show your supervisor.");
            }

            if (isCreateOrUpdate) {
                onCaseCreateUpdate(caseId);
            }
        }
    }

    private String getTrimmedElementTextOrBlank(TreeElement element) {
        if (element.getValue() == null) {
            return "";
        }

        return element.getValue().uncast().getString().trim();
    }


    private Case createCase(TreeElement createElement, Map<String, Case> currentOperatingSet,
                            String caseId, Date modified) throws InvalidStructureException {

        String[] data = new String[3];
        Case caseForBlock = null;

        for (int i = 0; i < createElement.getNumChildren(); i++) {
            TreeElement subElement = createElement.getChildAt(i);
            String tag = subElement.getName();
            switch (tag) {
                case "case_type":
                    data[0] = getTrimmedElementTextOrBlank(subElement);
                    break;
                case "owner_id":
                    data[1] = getTrimmedElementTextOrBlank(subElement);
                    break;
                case "case_name":
                    data[2] = getTrimmedElementTextOrBlank(subElement);
                    break;
                default:
                    throw new InvalidStructureException("Expected one of [case_type, owner_id, case_name], found " + tag);
            }
        }

        if (data[0] == null || data[2] == null) {
            throw new InvalidStructureException("One of [case_type, case_name] is missing for case <create> with ID: " + caseId);
        }

        caseForBlock = currentOperatingSet.get(caseId);

        if (caseForBlock != null) {
            caseForBlock.setName(data[2]);
            caseForBlock.setTypeId(data[0]);
        }

        if (caseForBlock == null) {
            // The case is either not present on the phone, or we're on strict tolerance
            caseForBlock = buildCase(data[2], data[0]);
            caseForBlock.setCaseId(caseId);
            caseForBlock.setDateOpened(modified);
        }

        if (data[1] != null) {
            caseForBlock.setUserId(data[1]);
        }
        return caseForBlock;
    }

    protected Case buildCase(String name, String typeId) {
        return new Case(name, typeId);
    }

    private Case loadCase(Case caseForBlock, String caseId, Map<String, Case> currentOperatingSet,
                          boolean errorIfMissing) throws InvalidStructureException {
        if (caseForBlock == null) {
            caseForBlock = currentOperatingSet.get(caseId);
        }
        if (errorIfMissing && caseForBlock == null) {
            throw new InvalidStructureException("Unable to update or close case " + caseId + ", it wasn't found");
        }
        return caseForBlock;
    }

    private void updateCase(TreeElement updateElement,
                            Case caseForBlock, String caseId) {

        for (int i = 0; i < updateElement.getNumChildren(); i++) {
            TreeElement subElement = updateElement.getChildAt(i);

            String key = subElement.getName();
            String value = getTrimmedElementTextOrBlank(subElement);

            switch (key) {
                case "case_type":
                    caseForBlock.setTypeId(value);
                    break;
                case "case_name":
                    caseForBlock.setName(value);
                    break;
                case "date_opened":
                    caseForBlock.setDateOpened(DateUtils.parseDate(value));
                    break;
                case "owner_id":
                    String oldUserId = caseForBlock.getUserId();

                    if (!oldUserId.equals(value)) {
                        onIndexDisrupted(caseId);
                    }
                    caseForBlock.setUserId(value);
                    break;
                default:
                    caseForBlock.setProperty(key, value);
                    break;
            }
        }
    }

    private void closeCase(Case caseForBlock, String caseId) {
        caseForBlock.setClosed(true);
        //this used to insist on a write happening _right here_. Not sure exactly why. Maybe related
        //to other writes happening in the same restore?
        onIndexDisrupted(caseId);
    }

    private void indexCase(TreeElement indexElement, Case caseForBlock, String caseId)
            throws InvalidStructureException {
        for (int i = 0; i < indexElement.getNumChildren(); i++) {
            TreeElement subElement = indexElement.getChildAt(i);

            String indexName = subElement.getName();
            String caseType = subElement.getAttributeValue(null, "case_type");

            String relationship = subElement.getAttributeValue(null, "relationship");
            if (relationship == null) {
                relationship = CaseIndex.RELATIONSHIP_CHILD;
            }

            String value = this.getTrimmedElementTextOrBlank(subElement);

            if (value.equals(caseId)) {
                throw new ActionableInvalidStructureException("case.error.self.index", new String[]{caseId}, "Case " + caseId + " cannot index itself");
            }

            //Remove any ambiguity associated with empty values
            if (value.equals("")) {
                value = null;
            }
            //Process blank inputs in the same manner as data fields (IE: Remove the underlying model)
            if (value == null) {
                if (caseForBlock.removeIndex(indexName)) {
                    onIndexDisrupted(caseId);
                }
            } else {
                if (caseForBlock.setIndex(new CaseIndex(indexName, caseType, value,
                        relationship))) {
                    onIndexDisrupted(caseId);
                }
            }
        }
    }

    /**
     * A signal that notes that processing a transaction has resulted in a
     * potential change in what cases should be on the phone. This can be
     * due to a case's owner changing, a case closing, an index moving, etc.
     *
     * Does not have to be consumed, but can be used to identify proactively
     * when to reconcile what cases should be available.
     *
     * @param caseId The ID of a case which has changed in a potentially
     *               disruptive way
     */
    public void onIndexDisrupted(String caseId) {

    }

    protected void onCaseCreateUpdate(String caseId) {

    }

    //These are unlikely to be used, and likely need to be refactored still a bit

    private void processCaseAttachment(TreeElement attachmentElement, Case caseForBlock) {
        for (int i = 0; i < attachmentElement.getNumChildren(); i++) {
            TreeElement subElement = attachmentElement.getChildAt(i);

            String attachmentName = subElement.getName();
            String src = subElement.getAttributeValue(null, "src");
            String from = subElement.getAttributeValue(null, "from");
            String fileName = subElement.getAttributeValue(null, "name");

            if ((src == null || "".equals(src)) && (from == null || "".equals(from))) {
                //this is actually an attachment removal
                removeAttachment(caseForBlock, attachmentName);
                caseForBlock.removeAttachment(attachmentName);
                continue;
            }

            String reference = processAttachment(src, from, fileName);
            if (reference != null) {
                caseForBlock.updateAttachment(attachmentName, reference);
            }
        }
    }

    protected void removeAttachment(Case caseForBlock, String attachmentName) {
        throw new RuntimeException("Attachment processing not available for bulk reads");
    }

    protected String processAttachment(String src, String from, String name) {
        throw new RuntimeException("Attachment processing not available for bulk reads");
    }
}

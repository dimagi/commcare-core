package org.commcare.xml;

import static org.commcare.xml.CaseXmlParserUtil.CASE_ATTACHMENT_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_CLOSE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_CREATE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_INDEX_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_ATTACHMENT_FROM;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_ATTACHMENT_NAME;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_ATTACHMENT_SRC;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CASE_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CASE_NAME;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CASE_TYPE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_CATEGORY;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_DATE_MODIFIED;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_DATE_OPENED;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_EXTERNAL_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_INDEX_CASE_TYPE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_INDEX_RELATIONSHIP;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_OWNER_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_STATE;
import static org.commcare.xml.CaseXmlParserUtil.CASE_PROPERTY_USER_ID;
import static org.commcare.xml.CaseXmlParserUtil.CASE_UPDATE_NODE;
import static org.commcare.xml.CaseXmlParserUtil.checkForMaxLength;
import static org.commcare.xml.CaseXmlParserUtil.validateMandatoryProperty;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.externalizable.SerializationLimitationException;
import org.javarosa.xml.util.ActionableInvalidStructureException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;
import java.util.NoSuchElementException;

/**
 * The CaseXML Parser is responsible for processing and performing
 * case transactions from an incoming XML stream. It will perform
 * all of the actions specified by the transaction (Create/modify/close)
 * against the application's current storage.
 *
 * NOTE: Future work on case XML Processing should shift to the BulkProcessingCaseXmlParser, since
 * there's no good way for us to maintain multiple different sources for all of the complex logic
 * inherent in this process. If anything would be added here, it should likely be replaced rather
 * than implemented in both places.
 *
 * @author ctsims
 */
public class CaseXmlParser extends TransactionParser<Case> implements CaseIndexChangeListener {

    private final IStorageUtilityIndexed storage;
    private final boolean acceptCreateOverwrites;

    public CaseXmlParser(KXmlParser parser, IStorageUtilityIndexed storage) {
        this(parser, true, storage);
    }

    /**
     * Creates a Parser for case blocks in the XML stream provided.
     *
     * @param parser                 The parser for incoming XML.
     * @param acceptCreateOverwrites Whether an Exception should be thrown if the transaction
     *                               contains create actions for cases which already exist.
     */
    public CaseXmlParser(KXmlParser parser, boolean acceptCreateOverwrites,
                         IStorageUtilityIndexed storage) {
        super(parser);

        this.acceptCreateOverwrites = acceptCreateOverwrites;
        this.storage = storage;
    }

    @Override
    public Case parse() throws InvalidStructureException, IOException, XmlPullParserException {
        checkNode(CASE_NODE);

        String caseId = parser.getAttributeValue(null, CASE_PROPERTY_CASE_ID);
        validateMandatoryProperty(CASE_PROPERTY_CASE_ID, caseId, "", parser);

        String dateModified = parser.getAttributeValue(null, CASE_PROPERTY_DATE_MODIFIED);
        validateMandatoryProperty(CASE_PROPERTY_DATE_MODIFIED, dateModified, caseId, parser);

        Date modified = DateUtils.parseDateTime(dateModified);

        String userId = parser.getAttributeValue(null, CASE_PROPERTY_USER_ID);

        Case caseForBlock = null;
        boolean isCreateOrUpdate = false;

        while (nextTagInBlock(CASE_NODE)) {
            String action = parser.getName().toLowerCase();
            switch (action) {
                case CASE_CREATE_NODE:
                    caseForBlock = createCase(caseId, modified, userId);
                    isCreateOrUpdate = true;
                    break;
                case CASE_UPDATE_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, true);
                    updateCase(caseForBlock, caseId);
                    isCreateOrUpdate = true;
                    break;
                case CASE_CLOSE_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, true);
                    closeCase(caseForBlock, caseId);
                    break;
                case CASE_INDEX_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, false);
                    indexCase(caseForBlock, caseId);
                    break;
                case CASE_ATTACHMENT_NODE:
                    caseForBlock = loadCase(caseForBlock, caseId, false);
                    processCaseAttachment(caseForBlock);
                    break;
            }
        }

        if (caseForBlock != null) {
            caseForBlock.setLastModified(modified);

            try {
                commit(caseForBlock);
            } catch (SerializationLimitationException e) {
                throw new InvalidStructureException("One of the property values for the case named '" +
                        caseForBlock.getName() + "' is too large (by " + e.percentOversized +
                        "%). Please show your supervisor.");
            }

            if (isCreateOrUpdate) {
                onCaseCreateUpdate(caseId);
            }
        }

        return null;
    }

    private Case createCase(String caseId, Date modified, String userId) throws InvalidStructureException, IOException, XmlPullParserException {
        String[] data = new String[3];
        Case caseForBlock = null;

        while (nextTagInBlock(CASE_CREATE_NODE)) {
            String tag = parser.getName();
            switch (tag) {
                case CASE_PROPERTY_CASE_TYPE:
                    data[0] = parser.nextText().trim();
                    break;
                case CASE_PROPERTY_OWNER_ID:
                    data[1] = parser.nextText().trim();
                    break;
                case CASE_PROPERTY_CASE_NAME:
                    data[2] = parser.nextText().trim();
                    break;
                default:
                    throw new InvalidStructureException("Expected one of [case_type, owner_id, case_name], found " + parser.getName(), parser);
            }
        }

        if (data[0] == null || data[2] == null) {
            throw new InvalidStructureException("One of [case_type, case_name] is missing for case <create> with ID: " + caseId, parser);
        }

        if (acceptCreateOverwrites) {
            caseForBlock = retrieve(caseId);

            if (caseForBlock != null) {
                caseForBlock.setName(data[2]);
                caseForBlock.setTypeId(data[0]);
            }
        }

        if (caseForBlock == null) {
            // The case is either not present on the phone, or we're on strict tolerance
            caseForBlock = buildCase(data[2], data[0]);
            caseForBlock.setCaseId(caseId);
            caseForBlock.setDateOpened(modified);
        }

        if (data[1] != null) {
            caseForBlock.setUserId(data[1]);
        } else {
            caseForBlock.setUserId(userId);
        }

        if (caseForBlock.getUserId() == null || caseForBlock.getUserId().contentEquals("")) {
            throw new InvalidStructureException("One of [user_id, owner_id] is missing for case <create> with ID: " + caseId, parser);
        }

        checkForMaxLength(caseForBlock);

        return caseForBlock;
    }

    private void updateCase(Case caseForBlock, String caseId) throws InvalidStructureException, IOException, XmlPullParserException {
        while (nextTagInBlock(CASE_UPDATE_NODE)) {
            String key = parser.getName();
            String value = parser.nextText().trim();

            switch (key) {
                case CASE_PROPERTY_CASE_TYPE:
                    caseForBlock.setTypeId(value);
                    break;
                case CASE_PROPERTY_CASE_NAME:
                    caseForBlock.setName(value);
                    break;
                case CASE_PROPERTY_DATE_OPENED:
                    caseForBlock.setDateOpened(DateUtils.parseDate(value));
                    break;
                case CASE_PROPERTY_OWNER_ID:
                    String oldUserId = caseForBlock.getUserId();

                    if (!oldUserId.equals(value)) {
                        onIndexDisrupted(caseId);
                    }
                    caseForBlock.setUserId(value);
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
                default:
                    caseForBlock.setProperty(key, value);
                    break;
            }
        }
        checkForMaxLength(caseForBlock);
    }

    private Case loadCase(Case caseForBlock, String caseId, boolean errorIfMissing) throws InvalidStructureException {
        if (caseForBlock == null) {
            caseForBlock = retrieve(caseId);
        }
        if (errorIfMissing && caseForBlock == null) {
            throw InvalidStructureException.readableInvalidStructureException("Unable to update or close case " + caseId + ", it wasn't found", parser);
        }
        return caseForBlock;
    }

    private void closeCase(Case caseForBlock, String caseId) throws IOException {
        caseForBlock.setClosed(true);
        commit(caseForBlock);
        onIndexDisrupted(caseId);
    }

    private void indexCase(Case caseForBlock, String caseId) throws InvalidStructureException, IOException, XmlPullParserException {
        while (nextTagInBlock(CASE_INDEX_NODE)) {
            String indexName = parser.getName();
            String caseType = parser.getAttributeValue(null, CASE_PROPERTY_INDEX_CASE_TYPE);

            String relationship = parser.getAttributeValue(null, CASE_PROPERTY_INDEX_RELATIONSHIP);
            if (relationship == null) {
                relationship = CaseIndex.RELATIONSHIP_CHILD;
            } else if ("".equals(relationship)) {
                throw new InvalidStructureException("Invalid Case Transaction: Attempt to create '' relationship type", parser);
            }

            String value = parser.nextText().trim();

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
                caseForBlock.setIndex(new CaseIndex(indexName, caseType, value, relationship));
                onIndexDisrupted(caseId);
            }
        }
    }

    private void processCaseAttachment(Case caseForBlock) throws InvalidStructureException, IOException, XmlPullParserException {
        while (nextTagInBlock(CASE_ATTACHMENT_NODE)) {
            String attachmentName = parser.getName();
            String src = parser.getAttributeValue(null, CASE_PROPERTY_ATTACHMENT_SRC);
            String from = parser.getAttributeValue(null, CASE_PROPERTY_ATTACHMENT_FROM);
            String fileName = parser.getAttributeValue(null, CASE_PROPERTY_ATTACHMENT_NAME);

            if ((src == null || "".equals(src)) && (from == null || "".equals(from))) {
                //this is actually an attachment removal
                removeAttachment(caseForBlock, attachmentName);
                caseForBlock.removeAttachment(attachmentName);
                continue;
            }

            String reference = processAttachment(src, from, fileName, parser);
            if (reference != null) {
                caseForBlock.updateAttachment(attachmentName, reference);
            }
        }
    }

    protected void removeAttachment(Case caseForBlock, String attachmentName) {

    }

    protected String processAttachment(String src, String from, String name, KXmlParser parser) {
        return null;
    }

    protected Case buildCase(String name, String typeId) {
        return new Case(name, typeId);
    }

    @Override
    protected void commit(Case parsed) throws IOException {
        storage().write(parsed);
    }

    protected Case retrieve(String entityId) {
        try {
            return (Case)storage().getRecordForValue(Case.INDEX_CASE_ID, entityId);
        } catch (NoSuchElementException nsee) {
            return null;
        }
    }

    public IStorageUtilityIndexed storage() {
        return storage;
    }

    @Override
    public void onIndexDisrupted(String caseId) {

    }

    protected void onCaseCreateUpdate(String caseId) {

    }
}

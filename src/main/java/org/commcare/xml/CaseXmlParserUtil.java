package org.commcare.xml;

import org.commcare.cases.model.Case;
import org.javarosa.xml.util.InvalidCasePropertyLengthException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;

public class CaseXmlParserUtil {
    public static final String CASE_NODE = "case";
    public static final String CASE_CREATE_NODE = "create";
    public static final String CASE_UPDATE_NODE = "update";
    public static final String CASE_CLOSE_NODE = "close";
    public static final String CASE_INDEX_NODE = "index";
    public static final String CASE_ATTACHMENT_NODE = "attachment";
    public static final String CASE_PROPERTY_CASE_ID = "case_id";
    public static final String CASE_PROPERTY_CASE_TYPE = "case_type";
    public static final String CASE_PROPERTY_OWNER_ID = "owner_id";
    public static final String CASE_PROPERTY_USER_ID = "user_id";
    public static final String CASE_PROPERTY_STATUS = "status";
    public static final String CASE_PROPERTY_CASE_NAME = "case_name";
    public static final String CASE_PROPERTY_LAST_MODIFIED = "last_modified";
    public static final String CASE_PROPERTY_DATE_MODIFIED = "date_modified";
    public static final String CASE_PROPERTY_DATE_OPENED = "date_opened";
    public static final String CASE_PROPERTY_EXTERNAL_ID = "external_id";
    public static final String CASE_PROPERTY_CATEGORY = "category";
    public static final String CASE_PROPERTY_STATE = "state";
    public static final String CASE_PROPERTY_INDEX = "index";
    public static final String CASE_PROPERTY_INDEX_CASE_TYPE = "case_type";
    public static final String CASE_PROPERTY_INDEX_RELATIONSHIP = "relationship";
    public static final String CASE_PROPERTY_ATTACHMENT_SRC = "src";
    public static final String CASE_PROPERTY_ATTACHMENT_FROM = "from";
    public static final String CASE_PROPERTY_ATTACHMENT_NAME = "name";


    public static void validateMandatoryProperty(String key, Object value, String caseId, KXmlParser parser) throws
            InvalidStructureException {
        if (value == null || value.equals("")) {
            String error = String.format("The %s attribute of a <case> %s wasn't set", key, caseId);
            throw  InvalidStructureException.readableInvalidStructureException(error, parser);
        }
    }

    protected static void checkForMaxLength(Case caseForBlock) throws InvalidStructureException {
        if (getStringLength(caseForBlock.getTypeId()) > 255) {
            throw new InvalidCasePropertyLengthException(CASE_PROPERTY_CASE_TYPE);
        } else if (getStringLength(caseForBlock.getUserId()) > 255) {
            throw new InvalidCasePropertyLengthException(CASE_PROPERTY_OWNER_ID);
        } else if (getStringLength(caseForBlock.getName()) > 255) {
            throw new InvalidCasePropertyLengthException(CASE_PROPERTY_CASE_NAME);
        } else if (getStringLength(caseForBlock.getExternalId()) > 255) {
            throw new InvalidCasePropertyLengthException(CASE_PROPERTY_EXTERNAL_ID);
        }
    }

    /**
     * Returns the length of string if it's not null, otherwise 0.
     */
    private static int getStringLength(String input) {
        return input != null ? input.length() : 0;
    }

}

package org.commcare.xml;

import org.commcare.cases.model.Case;
import org.javarosa.xml.util.InvalidCasePropertyLengthException;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;

public class CaseXmlParserUtil {
    public static final String CASE_NODE_NAME = "case";
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
    public static final String ATTACHMENT_FROM_LOCAL = "local";
    public static final String ATTACHMENT_FROM_REMOTE = "remote";
    public static final String ATTACHMENT_FROM_INLINE = "inline";
    public static final String CASE_XML_NAMESPACE = "http://commcarehq.org/case/transaction/v2";


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

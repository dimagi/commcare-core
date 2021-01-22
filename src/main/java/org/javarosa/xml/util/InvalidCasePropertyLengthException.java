package org.javarosa.xml.util;

public class InvalidCasePropertyLengthException extends InvalidStructureException {

    private final String caseProperty;

    public InvalidCasePropertyLengthException(String caseProperty) {
        super("Invalid <" + caseProperty + ">, value must be 255 characters or less");
        this.caseProperty = caseProperty;
    }

    public String getCaseProperty() {
        return caseProperty;
    }
}

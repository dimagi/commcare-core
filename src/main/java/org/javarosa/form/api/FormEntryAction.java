package org.javarosa.form.api;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Describes one form entry action used to replay form entry. 
 * Actions include value setting, repeat creation, and skipping over questions
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FormEntryAction implements Externalizable {
    private String questionRefString;
    private String value;
    private String action;
    private final static String NEW_REPEAT = "NEW_REPEAT";
    private final static String SKIP = "SKIP";
    private final static String VALUE = "VALUE";

    /**
     * For Externalization
     */
    public FormEntryAction() {
    }

    private FormEntryAction(String questionRefString, String value, String action) {
        this.questionRefString = questionRefString;
        this.value = value;
        this.action = action;
    }

    public static FormEntryAction buildNewRepeatAction(String questionRefString) {
        return new FormEntryAction(questionRefString, "", NEW_REPEAT);
    }

    public static FormEntryAction buildValueSetAction(String questionRefString, String value) {
        return new FormEntryAction(questionRefString, value, VALUE);
    }

    public static FormEntryAction buildSkipAction(String questionRefString) {
        return new FormEntryAction(questionRefString, "", SKIP);
    }

    public static FormEntryAction buildNullAction() {
        return new FormEntryAction("", "", "");
    }

    @Override
    public String toString() {
        if (NEW_REPEAT.equals(action)) {
            return "((" + questionRefString + ") (NEW_REPEAT))";
        } else if (VALUE.equals(action)) {
            // TODO PLM: escape 'value' field
            return "((" + questionRefString + ") (VALUE) (" + value + "))";
        } else if (SKIP.equals(action)) {
            return "((" + questionRefString + ") (SKIP))";
        } else {
            return "";
        }
    }

    @Override
    public int hashCode() {
        return questionRefString.hashCode() ^ value.hashCode() ^ action.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof FormEntryAction) {
            FormEntryAction otherAction = (FormEntryAction)other;
            return questionRefString.equals(otherAction.questionRefString) &&
                    value.equals(otherAction.value) &&
                    action.equals(otherAction.action);
        }
        return false;
    }

    public static FormEntryAction fromString(String entryActionString) {
        String unwrappedEntryActionString =
                entryActionString.substring(1, entryActionString.length() - 1);
        Vector<String> actionEntries = FormEntrySession.splitTopParens(unwrappedEntryActionString);
        int entryCount = actionEntries.size();

        if (entryCount != 2 && entryCount != 3) {
            throw new RuntimeException("Form entry action '" + entryActionString +
                    "' has an incorrect number of entries, expected 2 or 3, got " + entryCount);
        }

        String wrappedQuestionRefString = actionEntries.elementAt(0);
        String questionRefString = wrappedQuestionRefString.substring(1, wrappedQuestionRefString.length() - 1);
        if (entryCount == 2) {
            if (("(" + NEW_REPEAT + ")").equals(actionEntries.elementAt(1))) {
                return buildNewRepeatAction(questionRefString);
            } else {
                return buildSkipAction(questionRefString);
            }
        } else {
            String wrappedValue = actionEntries.elementAt(2);
            String value = wrappedValue.substring(1, wrappedValue.length() - 1);
            return buildValueSetAction(questionRefString, value);
        }
    }

    public boolean isNewRepeatAction() {
        return NEW_REPEAT.equals(action);
    }

    public boolean isSkipAction() {
        return SKIP.equals(action);
    }

    public String getValue() {
        return value;
    }

    public String getQuestionRefString() {
        return questionRefString;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        questionRefString = ExtUtil.readString(in);
        value = ExtUtil.readString(in);
        action = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, questionRefString);
        ExtUtil.writeString(out, value);
        ExtUtil.writeString(out, action);
    }
}


package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;

import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntrySession {
    private final Vector<FormEntryAction> actions = new Vector<FormEntryAction>();

    public void addNewRepeat(FormIndex formIndex) {
        actions.addElement(FormEntryAction.buildNewRepeatAction(formIndex));
    }

    public void addValueSet(FormIndex formIndex, String value) {
        actions.addElement(FormEntryAction.buildValueSetAction(formIndex, value));
    }

    @Override
    public String toString() {
        StringBuilder sessionStringBuilder = new StringBuilder();

        for (FormEntryAction formEntryAction : actions) {
            sessionStringBuilder.append(formEntryAction).append(" ");
        }

        return sessionStringBuilder.toString().trim();
    }

    public static FormEntrySession fromString(String sessionString) {
        FormEntrySession formEntrySession = new FormEntrySession();
        // TODO PLM: implement this, which is annoying, given need to parse/escape user data
        return formEntrySession;
    }

    public static class FormEntryAction {
        public final FormIndex formIndex;
        public final String value;
        public final boolean isNewRepeatAddition;

        private FormEntryAction(FormIndex formIndex, String value, boolean isNewRepeatAddition) {
            this.formIndex = formIndex;
            this.value = value;
            this.isNewRepeatAddition = isNewRepeatAddition;
        }

        public static FormEntryAction buildNewRepeatAction(FormIndex formIndex) {
            return new FormEntryAction(formIndex, "", true);
        }

        public static FormEntryAction buildValueSetAction(FormIndex formIndex, String value) {
            return new FormEntryAction(formIndex, value, false);
        }

        @Override
        public String toString() {
            if (isNewRepeatAddition) {
                // TODO PLM: escape 'value' field
                return "(" + formIndex.toString() + " " + value + ")";
            } else {
                return "(" + formIndex.toString() + " NEW_REPEAT)";
            }
        }
    }
}

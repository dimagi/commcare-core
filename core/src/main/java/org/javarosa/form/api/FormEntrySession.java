package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;

import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntrySession {
    private final Vector<FormEntryAction> actions = new Vector<FormEntryAction>();

    public void addNewRepeat(FormIndex formIndex) {
        actions.addElement(FormEntryAction.buildNewRepeatAction(formIndex.toString()));
    }

    public void addValueSet(FormIndex formIndex, String value) {
        actions.addElement(FormEntryAction.buildValueSetAction(formIndex.toString(), value));
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
        for (String actionString : FormEntrySession.splitTopParens(sessionString)) {
            formEntrySession.actions.addElement(FormEntryAction.fromString(actionString));
        }

        return formEntrySession;
    }

    public static class FormEntryAction {
        public final String formIndexString;
        public final String value;
        public final boolean isNewRepeatAddition;

        private FormEntryAction(String formIndexString, String value, boolean isNewRepeatAddition) {
            this.formIndexString = formIndexString;
            this.value = value;
            this.isNewRepeatAddition = isNewRepeatAddition;
        }

        public static FormEntryAction buildNewRepeatAction(String formIndexString) {
            return new FormEntryAction(formIndexString, "", true);
        }

        public static FormEntryAction buildValueSetAction(String formIndexString, String value) {
            return new FormEntryAction(formIndexString, value, false);
        }

        @Override
        public String toString() {
            if (isNewRepeatAddition) {
                return "((" + formIndexString + ") (NEW_REPEAT))";
            } else {
                // TODO PLM: escape 'value' field
                return "((" + formIndexString + ") (VALUE) (" + value + "))";
            }
        }

        public static FormEntryAction fromString(String entryActionString) {
            String unwrappedEntryActionString =
                    entryActionString.substring(1, entryActionString.length() - 1);
            Vector<String> actionEntries = splitTopParens(unwrappedEntryActionString);
            int entryCount = actionEntries.size();

            if (entryCount != 2 && entryCount != 3) {
                throw new RuntimeException();
            }

            String wrappedFormIndexString = actionEntries.get(0);
            String formIndexString = wrappedFormIndexString.substring(1, wrappedFormIndexString.length() - 1);
            if (entryCount == 2) {
                return buildNewRepeatAction(formIndexString);
            } else {
                String wrappedValue = actionEntries.get(2);
                String value = wrappedValue.substring(1, wrappedValue.length() - 1);
                return buildValueSetAction(formIndexString, value);
            }
        }
    }

    public static Vector<String> splitTopParens(String sessionString) {
        boolean wasEscapeChar = false;
        int parenDepth = 0;
        int topParenStart = 0;
        Vector<String> tokens = new Vector<String>();

        for(int i = 0, n = sessionString.length() ; i < n ; i++) {
            char c = sessionString.charAt(i);
            if (c == '\\') {
                wasEscapeChar = !wasEscapeChar;
            } else if ((c == '(' || c == ')') && wasEscapeChar) {
                wasEscapeChar = false;
            } else if (c == '(') {
                parenDepth++;
                if (parenDepth == 1) {
                    topParenStart = i;
                }
            } else if (c == ')') {
                if (parenDepth == 1) {
                    tokens.addElement(sessionString.substring(topParenStart, i+1));
                }
                parenDepth--;
            }
        }

        return tokens;
    }
}

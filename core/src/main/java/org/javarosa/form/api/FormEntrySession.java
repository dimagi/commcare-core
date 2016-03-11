package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Records form entry actions, associating form indexes with user (string)
 * answers.  For simplicity's sake each form index appears only once in the
 * action list. Updating an answer does not change its ordering in the action list.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntrySession implements FormEntrySessionRecorder, Externalizable {
    private Vector<FormEntryAction> actions = new Vector<FormEntryAction>();

    /**
     * For Externalization
     */
    public FormEntrySession() {
    }

    @Override
    public void addNewRepeat(FormIndex formIndex) {
        final String formIndexString = formIndex.toString();
        int insertIndex = removeDuplicateAction(formIndexString);
        actions.insertElementAt(FormEntryAction.buildNewRepeatAction(formIndexString), insertIndex);
    }

    private int removeDuplicateAction(String formIndexString) {
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.elementAt(i).getFormIndexString().equals(formIndexString)) {
                actions.removeElementAt(i);
                return i;
            }
        }
        return actions.size();
    }

    @Override
    public void addValueSet(FormIndex formIndex, String value) {
        final String formIndexString = formIndex.toString();
        int insertIndex = removeDuplicateAction(formIndexString);
        actions.insertElementAt(FormEntryAction.buildValueSetAction(formIndexString, value), insertIndex);
    }

    @Override
    public void addQuestionSkip(FormIndex formIndex) {
        final String formIndexString = formIndex.toString();
        int insertIndex = removeDuplicateAction(formIndexString);
        actions.insertElementAt(FormEntryAction.buildSkipAction(formIndexString), insertIndex);
    }

    public FormEntryAction popAction() {
        if (actions.size() > 0) {
            FormEntryAction firstAction = actions.firstElement();
            actions.removeElementAt(0);
            return firstAction;
        } else {
            return FormEntryAction.buildNullAction();
        }
    }

    public FormEntryAction peekAction() {
        if (actions.size() > 0) {
            return actions.elementAt(0);
        } else {
            return FormEntryAction.buildNullAction();
        }
    }

    public int size() {
        return actions.size();
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
        for (String actionString : splitTopParens(sessionString)) {
            formEntrySession.actions.addElement(FormEntryAction.fromString(actionString));
        }

        return formEntrySession;
    }

    public static Vector<String> splitTopParens(String sessionString) {
        boolean wasEscapeChar = false;
        int parenDepth = 0;
        int topParenStart = 0;
        Vector<String> tokens = new Vector<String>();

        for (int i = 0, n = sessionString.length(); i < n; i++) {
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
                    tokens.addElement(sessionString.substring(topParenStart, i + 1));
                }
                parenDepth--;
            }
        }

        return tokens;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        actions = (Vector<FormEntryAction>)ExtUtil.read(in, new ExtWrapList(FormEntryAction.class), pf);

    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapList(actions));
    }
}

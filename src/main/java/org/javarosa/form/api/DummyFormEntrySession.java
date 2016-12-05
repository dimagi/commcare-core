package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;

/**
 * Empty form entry session implementation, useful when you don't want to
 * record form entry actions
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class DummyFormEntrySession implements FormEntrySessionRecorder {
    @Override
    public void addNewRepeat(FormIndex formIndex) {

    }

    @Override
    public void addValueSet(FormIndex formIndex, String value) {

    }

    @Override
    public void addQuestionSkip(FormIndex formIndex) {

    }

    @Override
    public String toString() {
        return "";
    }
}

package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;

/**
 * Record form entry actions for playback
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public interface FormEntrySessionRecorder {
    void addNewRepeat(FormIndex formIndex);
    void addValueSet(FormIndex formIndex, String value);
    void addQuestionSkip(FormIndex formIndex);
    String toString();
}

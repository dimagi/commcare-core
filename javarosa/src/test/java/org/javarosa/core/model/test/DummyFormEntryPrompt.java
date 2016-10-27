package org.javarosa.core.model.test;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.form.api.FormEntryPrompt;

public class DummyFormEntryPrompt extends FormEntryPrompt {
    final String textId;
    final Localizer localizer;

    public DummyFormEntryPrompt(Localizer localizer, String textId, QuestionDef q) {
        this.localizer = localizer;
        this.textId = textId;
        this.element = q;
    }

    @Override
    protected String getTextID() {
        return textId;
    }

    @Override
    public Localizer localizer() {
        return localizer;
    }

    @Override
    protected String substituteStringArgs(String template) {
        return template;
    }
}

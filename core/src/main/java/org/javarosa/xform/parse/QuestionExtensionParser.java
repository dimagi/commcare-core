package org.javarosa.xform.parse;

import org.javarosa.core.model.QuestionDataExtension;
import org.kxml2.kdom.Element;

/**
 * A parser for some additional piece of information included with a question in an xform that
 * is not handled by the question type itself. There should exist a 1-to-1 relationship between
 * an implementing class of this class, and an implementing class of QuestionDataExtension, i.e.
 * 1 QuestionExtensionParser creates 1 QuestionDataExtension
 */
public abstract class QuestionExtensionParser {

    // The name of the question type that this parser is for ("input", "upload", etc.)
    private String elementName;

    public QuestionExtensionParser(String elementName) {
        this.elementName = elementName;
    }

    public boolean canParse(Element e) {
        return e.getName().equals(elementName);
    }

    // May return null if the specific extension data being sought is not present for the given
    // element
    public abstract QuestionDataExtension parse(Element e);
}

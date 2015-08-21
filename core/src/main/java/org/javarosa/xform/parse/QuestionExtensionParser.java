package org.javarosa.xform.parse;

import org.javarosa.core.model.QuestionDataExtension;
import org.kxml2.kdom.Element;

/**
 * Created by amstone326 on 8/20/15.
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

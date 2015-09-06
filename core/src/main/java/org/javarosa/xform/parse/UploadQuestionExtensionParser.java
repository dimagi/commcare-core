package org.javarosa.xform.parse;

import org.javarosa.core.model.QuestionDataExtension;
import org.javarosa.core.model.UploadQuestionExtension;

import org.kxml2.kdom.Element;

/**
 * An additional parser for the "upload" question type, which can be used to parse any additional
 * attributes included in an upload question.
 *
 * @author amstone
 */
public class UploadQuestionExtensionParser extends QuestionExtensionParser {

    public UploadQuestionExtensionParser() {
        setElementName("upload");
    }

    public QuestionDataExtension parse(Element elt) {
        String s = elt.getAttributeValue(XFormParser.NAMESPACE_JAVAROSA,
                "imageDimensionScaledMax");
        if (s != null) {
            if (s.endsWith("px")) {
                s = s.substring(0, s.length() - 2);
            }
            try {
                int maxDimens = Integer.parseInt(s);
                return new UploadQuestionExtension(maxDimens);
            } catch (NumberFormatException e) {
                throw new XFormParseException("Invalid input for image max dimension: " + s);
            }
        }
        return null;
    }

    public String[] getUsedAttributes() {
        return new String[]{"imageDimensionScaledMax"};
    }
}

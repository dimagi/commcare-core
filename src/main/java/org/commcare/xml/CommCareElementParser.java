package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Text;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Element parser extended with parsing function(s) that create CommCare
 * specific model objects.
 *
 * @author Phillip Mates
 */
public abstract class CommCareElementParser<T> extends ElementParser<T> {

    /**
     * The profile is incompatible with the major version of the current CommCare installation *
     */
    public static final int REQUIREMENT_MAJOR_APP_VERSION = 1;
    /**
     * The profile is incompatible with the minor version of the current CommCare installation *
     */
    public static final int REQUIREMENT_MINOR_APP_VERSION = 2;

    public CommCareElementParser(KXmlParser parser) {
        super(parser);
    }

    /**
     * Build a DisplayUnit object by parsing the contents of a display tag.
     */
    public DisplayUnit parseDisplayBlock() throws InvalidStructureException, IOException, XmlPullParserException {
        Text imageValue = null;
        Text audioValue = null;
        Text displayText = null;
        Text badgeText = null;

        while (nextTagInBlock("display")) {
            if (parser.getName().equals("text")) {
                String attributeValue = parser.getAttributeValue(null, "form");
                if ("image".equals(attributeValue)) {
                    imageValue = new TextParser(parser).parse();
                } else if ("audio".equals(attributeValue)) {
                    audioValue = new TextParser(parser).parse();
                } else if ("badge".equals(attributeValue) ) {
                    badgeText = new TextParser(parser).parse();
                } else {
                    displayText = new TextParser(parser).parse();
                }
            } else if ("media".equals(parser.getName())) {
                String imagePath = parser.getAttributeValue(null, "image");
                if (imagePath != null) {
                    imageValue = Text.PlainText(imagePath);
                }

                String audioPath = parser.getAttributeValue(null, "audio");
                if (audioPath != null) {
                    audioValue = Text.PlainText(audioPath);
                }
                //only ends up grabbing the last entries with
                //each attribute, but we can only use one of each anyway.
            }
        }

        return new DisplayUnit(displayText, imageValue, audioValue, badgeText);
    }
}

package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Text;
import org.javarosa.xform.parse.ElementParser;
import org.javarosa.xform.util.InvalidStructureException;
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

    public CommCareElementParser(KXmlParser parser) {
        super(parser);
    }

    /**
     * Build a DisplayUnit object by parsing the contents of a display tag.
     */
    public DisplayUnit parseDisplayBlock() throws InvalidStructureException, IOException, XmlPullParserException {
        String imageValue = null;
        String audioValue = null;
        Text displayText = null;

        while (nextTagInBlock("display")) {
            if (parser.getName().equals("text")) {
                displayText = new TextParser(parser).parse();
            }
            //check and parse media stuff
            else if (parser.getName().equals("media")) {
                imageValue = parser.getAttributeValue(null, "image");
                audioValue = parser.getAttributeValue(null, "audio");
                //only ends up grabbing the last entries with
                //each attribute, but we can only use one of each anyway.
            }
        }

        return new DisplayUnit(displayText, imageValue, audioValue);
    }
}

package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Element parser extended with parsing function(s) that create CommCare specific model objects
 * @author Phillip Mates
 */
public abstract class CommCareElementParser<T> extends ElementParser<T> {

    public CommCareElementParser(KXmlParser parser) {
        super(parser);
    }

    public DisplayUnit parseDisplayBlock() throws InvalidStructureException, IOException, XmlPullParserException {
        Object[] info = new Object[3];
        while (nextTagInBlock("display")) {
            if (parser.getName().equals("text")) {
                info[0] = new TextParser(parser).parse();
            }
            //check and parse media stuff
            else if (parser.getName().equals("media")) {
                info[1] = parser.getAttributeValue(null, "image");
                info[2] = parser.getAttributeValue(null, "audio");
                //only ends up grabbing the last entries with
                //each attribute, but we can only use one of each anyway.
            }
        }

        return new DisplayUnit((Text)info[0], (String)info[1], (String)info[2]);
    }
}

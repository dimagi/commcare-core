package org.commcare.xml;

import org.commcare.suite.model.Callout;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Parser used in DetailParser to parse the Grid attributes for a GridEntityView
 *
 * @author wspride
 */

public class CalloutParser extends ElementParser<Callout> {

    public CalloutParser(KXmlParser parser) {
        super(parser);
    }

    public Callout parse() throws InvalidStructureException, IOException, XmlPullParserException {

        System.out.println("Parsing Callout!");

        String text = parser.getName();
        String actionName = parser.getAttributeValue(null, "action");
        String image = parser.getAttributeValue(null, "image");

        Callout callout = new Callout(actionName, image);

        while(nextTagInBlock(text)){
            String tagname = parser.getName();
            if(tagname.equals("extra")){
                callout.addExtra(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                System.out.println("added extra with key: " + parser.getAttributeValue(null, "key"));
            } else if (tagname.equals("response")){
                callout.addResponse(parser.getAttributeValue(null, "key"));
                System.out.println("added response with key: " + parser.getAttributeValue(null, "key"));
            }

        }
        //exit grid block
        parser.nextTag();

        return callout;
    }
}

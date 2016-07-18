package org.commcare.xml;


import org.commcare.suite.model.Alert;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import org.kxml2.io.KXmlParser;
import java.io.IOException;

/**
 * Created by Saumya on 7/8/2016.
 */
public class AlertParser extends CommCareElementParser<Alert> {

    public AlertParser(KXmlParser parser){
        super(parser);
    }

    @Override
    public Alert parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException {

        String condition = "";
        String type = "";

        while (nextTagInBlock("alert")) {
            String tagName = parser.getName();

            if(tagName.toLowerCase().equals("exp")){
                condition = parser.nextText();
            }
            else if(tagName.toLowerCase().equals("type")){
                type = parser.nextText();
            }
        }

        return new Alert(type, condition);
    }
}

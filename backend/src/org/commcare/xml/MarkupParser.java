package org.commcare.xml;

import org.commcare.suite.model.DetailField.Builder;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class MarkupParser extends ElementParser<Integer> {

    final Builder builder;

    public MarkupParser(Builder builder, KXmlParser parser) {
        super(parser);
        this.builder = builder;
    }

    public Integer parse() throws InvalidStructureException, IOException, XmlPullParserException {

        parser.nextTag();

        checkNode("css");
        String id = parser.getAttributeValue(null, "id");
        builder.setCssID(id);

        //exit grid block
        parser.nextTag();

        return new Integer(1);
    }
}

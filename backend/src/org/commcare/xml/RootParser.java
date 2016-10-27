package org.commcare.xml;

import org.javarosa.core.reference.RootTranslator;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * @author ctsims
 */
public class RootParser extends ElementParser<RootTranslator> {

    public RootParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public RootTranslator parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("root");

        String id = parser.getAttributeValue(null, "prefix");
        String readonly = parser.getAttributeValue(null, "readonly");

        //Get the child or error out if none exists
        getNextTagInBlock("root");

        String referenceType = parser.getName().toLowerCase();
        String path = parser.getAttributeValue(null, "path");
        if (referenceType.equals("filesystem")) {
            return new RootTranslator("jr://" + id + "/", "jr://file" + path);
        } else if (referenceType.equals("resource")) {
            return new RootTranslator("jr://" + id + "/", "jr://resource" + path);
        } else if (referenceType.equals("absolute")) {
            return new RootTranslator("jr://" + id + "/", path);
        } else {
            throw new InvalidStructureException("No available reference types to parse out reference root " + referenceType, parser);
        }
    }

}

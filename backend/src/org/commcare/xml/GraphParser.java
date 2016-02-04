package org.commcare.xml;

import org.commcare.suite.model.DetailTemplate;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Parser for a <graph> element, typically used as a detail field's template.
 *
 * @author jschweers
 */
public abstract class GraphParser extends ElementParser<DetailTemplate> {
    public GraphParser(KXmlParser parser) {
        super(parser);
    }

    public abstract DetailTemplate parse() throws InvalidStructureException, IOException, XmlPullParserException;
}

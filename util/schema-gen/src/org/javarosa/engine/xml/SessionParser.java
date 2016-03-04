package org.javarosa.engine.xml;

import org.javarosa.engine.models.Session;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * @author ctsims
 */
public class SessionParser extends ElementParser<Session> {

    public SessionParser(KXmlParser parser) throws IOException {
        super(parser);
    }

    @Override
    public Session parse() throws InvalidStructureException, IOException, XmlPullParserException {
        this.checkNode("session");
        this.skipBlock("session");
        return null;
    }
}

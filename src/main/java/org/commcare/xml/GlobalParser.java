package org.commcare.xml;

import org.commcare.suite.model.GeoOverlay;
import org.commcare.suite.model.Global;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

class GlobalParser extends ElementParser<Global> {

    static final String NAME_GLOBAL = "global";

    GlobalParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public Global parse() throws InvalidStructureException, IOException, XmlPullParserException {
        Vector<GeoOverlay> geoOverlays = new Vector<>();
        while (nextTagInBlock(NAME_GLOBAL)) {
            if (GeoOverlayParser.NAME_GEO_OVERLAY.equals(parser.getName().toLowerCase())) {
                GeoOverlay geoOverlay = new GeoOverlayParser(parser).parse();
                geoOverlays.add(geoOverlay);
            }
        }
        return new Global(geoOverlays);
    }
}

package org.commcare.xml;

import org.commcare.suite.model.DisplayUnit;
import org.commcare.suite.model.GeoOverlay;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Parses the defintion for a {@code GeoOverlay} element
 */
public class GeoOverlayParser extends ElementParser<GeoOverlay> {

    static final String NAME_GEO_OVERLAY = "geo-overlay";
    private static final String NAME_COORDINATES = "coordinates";
    private static final String NAME_TITLE = "title";

    GeoOverlayParser(KXmlParser parser) {
        super(parser);
    }

    @Override
    public GeoOverlay parse() throws InvalidStructureException, IOException, XmlPullParserException {
        DisplayUnit title = null;
        DisplayUnit coordinates = null;
        while (nextTagInBlock(NAME_GEO_OVERLAY)) {
            String tagName = parser.getName().toLowerCase();
            if (NAME_COORDINATES.contentEquals(tagName)) {
                nextTagInBlock(NAME_COORDINATES);
                coordinates = new DisplayUnit(new TextParser(parser).parse());
            } else if (NAME_TITLE.contentEquals(tagName)) {
                nextTagInBlock(NAME_TITLE);
                title = new DisplayUnit(new TextParser(parser).parse());
            }
        }
        return new GeoOverlay(title, coordinates);
    }
}

package org.commcare.xml;

import org.commcare.suite.model.DetailField.Builder;
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

public class GridParser extends ElementParser<Integer> {

    final Builder builder;

    public GridParser(Builder builder, KXmlParser parser) {
        super(parser);
        this.builder = builder;
    }

    @Override
    public Integer parse() throws InvalidStructureException, IOException, XmlPullParserException {

        checkNode("grid");
        String gridx = parser.getAttributeValue(null, "grid-x");
        builder.setGridX(Integer.parseInt(gridx));

        String gridy = parser.getAttributeValue(null, "grid-y");
        builder.setGridY(Integer.parseInt(gridy));

        String gridw = parser.getAttributeValue(null, "grid-width");
        builder.setGridWidth(Integer.parseInt(gridw));

        String gridh = parser.getAttributeValue(null, "grid-height");
        builder.setGridHeight(Integer.parseInt(gridh));

        //exit grid block
        parser.nextTag();

        return new Integer(1);
    }
}

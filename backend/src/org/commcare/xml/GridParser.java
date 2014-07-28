package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.DetailField.Builder;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class GridParser extends ElementParser<Void> {
	
	Builder builder;
	
	public GridParser(Builder builder, KXmlParser parser) {
		super(parser);
		this.builder = builder;
	}	
	
	public Void parse() throws InvalidStructureException, IOException, XmlPullParserException {
		
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
		
		return element;
	}
}

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

public class StyleParser extends ElementParser<Void> {
	
	Builder builder;
	
	public StyleParser(Builder builder, KXmlParser parser) {
		super(parser);
		this.builder = builder;
	}	
	
	public Void parse() throws InvalidStructureException, IOException, XmlPullParserException {
		
		String fontSize = parser.getAttributeValue(null, "font-size");
		builder.setFontSize(fontSize);

		String horzAlign = parser.getAttributeValue(null, "horz-align");
		builder.setHorizontalAlign(horzAlign);

		String vertAlign = parser.getAttributeValue(null, "vert-align");
		builder.setVerticalAlign(vertAlign);
		//exit style block
		parser.nextTag();
		
		return element;
	}
}

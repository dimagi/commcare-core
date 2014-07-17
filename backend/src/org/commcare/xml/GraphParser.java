package org.commcare.xml;

import java.io.IOException;

import org.commcare.suite.model.Graph;
import org.commcare.suite.model.Series;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class GraphParser extends ElementParser<Graph> {
	public GraphParser(KXmlParser parser) {
		super(parser);
	}	
	
	public Graph parse() throws InvalidStructureException, IOException, XmlPullParserException {
		Graph graph = new Graph();
		
		int entryLevel = parser.getDepth();
		do {
			parser.nextTag();
			if (parser.getName().equals("series")) {
				graph.addSeries(parseSeries());
			}
		} while (parser.getDepth() > entryLevel);
		
		return graph;
	}
	
	private Series parseSeries() throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("series");
		String nodeSet = parser.getAttributeValue(null, "nodeset");
		
		nextStartTag();
		checkNode("x");
		String x = parser.getAttributeValue(null,"function");
		
		nextStartTag();
		checkNode("y");
		String y = parser.getAttributeValue(null,"function");

		while (parser.getEventType() != KXmlParser.END_TAG || !parser.getName().equals("series")) {
			parser.nextTag();
		}
		return new Series(nodeSet, x, y);
	}
	
	private void nextStartTag() throws IOException, XmlPullParserException {
		do {
			parser.nextTag();
		} while (parser.getEventType() != KXmlParser.START_TAG);
	}
}

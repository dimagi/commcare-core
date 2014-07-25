package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;

import org.commcare.suite.model.GraphTemplate;
import org.commcare.suite.model.Series;
import org.commcare.suite.model.Text;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

public class GraphParser extends ElementParser<GraphTemplate> {
	public GraphParser(KXmlParser parser) {
		super(parser);
	}	
	
	public GraphTemplate parse() throws InvalidStructureException, IOException, XmlPullParserException {
		GraphTemplate graph = new GraphTemplate();
		String type = parser.getAttributeValue(null, "type");
		if (type == null) {
			throw new InvalidStructureException("Expected attribute @type for element <" +  parser.getName() + ">", parser);
		}
		graph.setType(type);
		
		int entryLevel = parser.getDepth();
		do {
			parser.nextTag();
			if (parser.getName().equals("configuration")) {
				parseConfiguration(graph);
			}
			if (parser.getName().equals("series")) {
				graph.addSeries(parseSeries(type));
			}
		} while (parser.getDepth() > entryLevel);
		
		return graph;
	}
	
	private void parseConfiguration(GraphTemplate graph) throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("configuration");
		
		TextParser textParser = new TextParser(parser);
		do {
			parser.nextTag();
			if (parser.getName().equals("text")) {
				String id = parser.getAttributeValue(null, "id");
				Text t = textParser.parse();
				graph.setConfiguration(id, t);
			}
		} while (parser.getEventType() != KXmlParser.END_TAG || !parser.getName().equals("configuration"));
	}
	
	private Series parseSeries(String type) throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("series");
		String nodeSet = parser.getAttributeValue(null, "nodeset");
		
		nextStartTag();
		checkNode("x");
		String x = parser.getAttributeValue(null,"function");
		
		nextStartTag();
		checkNode("y");
		String y = parser.getAttributeValue(null,"function");

		String radius = null;
		if (type.equals(GraphTemplate.TYPE_BUBBLE)) {
			nextStartTag();
			checkNode("radius");
			radius = parser.getAttributeValue(null, "function");
		}

		while (parser.getEventType() != KXmlParser.END_TAG || !parser.getName().equals("series")) {
			parser.nextTag();
		}
		
		return new Series(nodeSet, x, y, radius);
	}
	
	private void nextStartTag() throws IOException, XmlPullParserException {
		do {
			parser.nextTag();
		} while (parser.getEventType() != KXmlParser.START_TAG);
	}
}

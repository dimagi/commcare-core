package org.commcare.xml;

import java.io.IOException;
import java.util.Hashtable;

import org.commcare.suite.model.Annotation;
import org.commcare.suite.model.GraphTemplate;
import org.commcare.suite.model.Series;
import org.commcare.suite.model.Text;
import org.commcare.suite.model.graph.Configurable;
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
			if (parser.getName().equals("annotation")) {
				parseAnnotation(graph);
			}
		} while (parser.getDepth() > entryLevel);
		
		return graph;
	}
	
	private void parseAnnotation(GraphTemplate graph) throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("annotation");
		
		TextParser textParser = new TextParser(parser);
		
		nextStartTag();
		checkNode("x");
		nextStartTag();
		Text x = textParser.parse();
		
		nextStartTag();
		checkNode("y");
		nextStartTag();
		Text y = textParser.parse();
		
		nextStartTag();
		Text text = textParser.parse();
		
		parser.nextTag();
		
		graph.addAnnotation(new Annotation(x, y, text));
	}
	
	private void parseConfiguration(Configurable data) throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("configuration");
		
		TextParser textParser = new TextParser(parser);
		do {
			parser.nextTag();
			if (parser.getName().equals("text")) {
				String id = parser.getAttributeValue(null, "id");
				Text t = textParser.parse();
				data.setConfiguration(id, t);
			}
		} while (parser.getEventType() != KXmlParser.END_TAG || !parser.getName().equals("configuration"));
	}
	
	private Series parseSeries(String type) throws InvalidStructureException, IOException, XmlPullParserException {
		checkNode("series");
		String nodeSet = parser.getAttributeValue(null, "nodeset");
		Series series = new Series(nodeSet);
		
		nextStartTag();
		if (parser.getName().equals("configuration")) {
			parseConfiguration(series);
			nextStartTag();
		}
		
		checkNode("x");
		series.setX(parser.getAttributeValue(null,"function"));
		
		nextStartTag();
		checkNode("y");
		series.setY(parser.getAttributeValue(null,"function"));

		if (type.equals(GraphTemplate.TYPE_BUBBLE)) {
			nextStartTag();
			checkNode("radius");
			series.setRadius(parser.getAttributeValue(null, "function"));
		}

		while (parser.getEventType() != KXmlParser.END_TAG || !parser.getName().equals("series")) {
			parser.nextTag();
		}
		
		return series;
	}
	
	private void nextStartTag() throws IOException, XmlPullParserException {
		do {
			parser.nextTag();
		} while (parser.getEventType() != KXmlParser.START_TAG);
	}
}

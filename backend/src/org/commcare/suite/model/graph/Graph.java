package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.DetailTemplate;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class Graph implements Externalizable, DetailTemplate, Configurable {
	public static final String TYPE_XY = "xy";
	public static final String TYPE_BUBBLE = "bubble";

	private String type;
	private Vector<Series> series;
	private Hashtable<String, Text> configuration;
	private Vector<Annotation> annotations;
	
	public Graph() {
		series = new Vector<Series>();
		configuration = new Hashtable<String, Text>();
		annotations = new Vector<Annotation>();
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void addSeries(Series s) {
		series.addElement(s);
	}
	
	public void addAnnotation(Annotation a) {
		annotations.addElement(a);
	}
	
	public Text getConfiguration(String key) {
		return configuration.get(key);
	}
	
	public void setConfiguration(String key, Text value) {
		configuration.put(key, value);
	}

	public Enumeration getConfigurationKeys() {
		return configuration.keys();
	}
	
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub
	}

	public GraphData evaluate(EvaluationContext context) {
		GraphData data = new GraphData();
		data.setType(type);
		evaluateSeries(data, context);
		evaluateAnnotations(data, context);
		evaluateConfiguration(this, data, context);
		return data;
	}
	
	private void evaluateAnnotations(GraphData graphData, EvaluationContext context) {
		for (Annotation a : annotations) {
			graphData.addAnnotation(new PointData(
				Double.valueOf(a.getX().evaluate(context)), 
				Double.valueOf(a.getY().evaluate(context)), 
				a.getAnnotation().evaluate(context)
			));
		}
	}
	
	private void evaluateConfiguration(Configurable template, ConfigurableData data, EvaluationContext context) {
		Enumeration e = template.getConfigurationKeys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			data.setConfiguration(key, template.getConfiguration(key).evaluate(context));
		}
	}
	
	private XPathExpression parseExpression(String function) throws XPathSyntaxException {
		if (function == null) {
			return null;
		}
		return XPathParseTool.parseXPath("string(" + function + ")");
	}
	
	private Double evaluateExpression(XPathExpression expression, EvaluationContext context) {
		if (expression != null) {
			String value = (String) expression.eval(context.getMainInstance(), context);
			if (value.length() > 0) {
				return Double.valueOf(value);
			}
		}
		return null;
	}
	
	private void evaluateSeries(GraphData graphData, EvaluationContext context) {
		try {
			for (Series s : series) {
				XPathExpression xParse = parseExpression(s.getX());
				XPathExpression yParse = parseExpression(s.getY());
				XPathExpression radiusParse = parseExpression(s.getRadius());
				
				Vector<TreeReference> refList = context.expandReference(s.getNodeSet());
				SeriesData seriesData = new SeriesData();
				evaluateConfiguration(s, seriesData, context);
				for (TreeReference ref : refList) {
					EvaluationContext refContext = new EvaluationContext(context, ref);
					Double x = evaluateExpression(xParse, refContext);
					Double y = evaluateExpression(yParse, refContext);
					Double radius = evaluateExpression(radiusParse, refContext);
					if (x != null && y != null) {
						if (radius != null) {
							seriesData.addPoint(new PointData(x, y, radius));
						}
						else {
							seriesData.addPoint(new PointData(x, y));
						}
					}
				}
				graphData.addSeries(seriesData);
			}
		}
		catch (XPathSyntaxException e) {
			e.printStackTrace();
		}
	}

}

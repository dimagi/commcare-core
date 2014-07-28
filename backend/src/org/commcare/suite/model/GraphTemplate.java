package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.graph.Configurable;
import org.commcare.suite.model.graph.ConfigurableData;
import org.commcare.suite.model.graph.GraphData;
import org.commcare.suite.model.graph.PointData;
import org.commcare.suite.model.graph.SeriesData;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class GraphTemplate implements Externalizable, IDetailTemplate, Configurable {
	public static final String TYPE_LINE = "line";
	public static final String TYPE_BUBBLE = "bubble";

	private String type;
	private Vector<Series> series;
	private Hashtable<String, Text> configuration;
	private Vector<Annotation> annotations;
	
	public GraphTemplate() {
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
	
	private void evaluateSeries(GraphData graphData, EvaluationContext context) {
		try {
			for (Series s : series) {
				Hashtable<String, String> functions = new Hashtable<String, String>(3);
				functions.put("x", s.getX());
				functions.put("y", s.getY());
				if (s.getRadius() != null) {
					functions.put("radius", s.getRadius());
				}
				Hashtable<String, XPathExpression> parse = new Hashtable<String, XPathExpression>(functions.size());
				Enumeration e = functions.keys();
				while (e.hasMoreElements()) {
					String dimension = (String) e.nextElement();
					String function = functions.get(dimension);
					if (function != null) {
						parse.put(dimension, XPathParseTool.parseXPath("string(" + function + ")"));
					}
				}
				
				Vector<TreeReference> refList = context.expandReference(s.getNodeSet());
				SeriesData seriesData = new SeriesData();
				evaluateConfiguration(s, seriesData, context);
				for (TreeReference ref : refList) {
					EvaluationContext refContext = new EvaluationContext(context, ref);
					Enumeration f = parse.keys();
					Hashtable<String, Double> doubles = new Hashtable<String, Double>(parse.size());
					while (f.hasMoreElements()) {
						String dimension = (String) f.nextElement();
						String value = (String) parse.get(dimension).eval(refContext.getMainInstance(), refContext);
						if (value.length() > 0) {
							doubles.put(dimension, Double.valueOf(value));
						}
					}
					if (doubles.containsKey("x") && doubles.containsKey("y")) {
						if (doubles.containsKey("radius")) {
							seriesData.addPoint(new PointData(doubles.get("x"), doubles.get("y"), doubles.get("radius")));
						}
						else {
							seriesData.addPoint(new PointData(doubles.get("x"), doubles.get("y")));
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

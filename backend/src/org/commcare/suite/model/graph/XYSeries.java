package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XYSeries implements Externalizable, Configurable {
	private TreeReference nodeSet;
	private Hashtable<String, Text> configuration;
	
	private String x;
	private String y;
	
	private XPathExpression xParse;
	private XPathExpression yParse;
	
	public XYSeries(String nodeSet) {
		this.nodeSet = XPathReference.getPathExpr(nodeSet).getReference(true);
		this.configuration = new Hashtable<String, Text>();
	}
	
	public TreeReference getNodeSet() {
		return nodeSet;
	}
	
	public String getX() {
		return x;
	}
	
	public void setX(String x) {
		this.x = x;
		this.xParse = null;
	}
	
	public String getY() {
		return y;
	}
	
	public void setY(String y) {
		this.y = y;
		this.yParse = null;
	}
	
	public void setConfiguration(String key, Text value) {
		configuration.put(key, value);
	}
	
	public Text getConfiguration(String key) {
		return configuration.get(key);
	}

	public Enumeration getConfigurationKeys() {
		return configuration.keys();
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}
	
	protected void parse() throws XPathSyntaxException {
		if (xParse == null) {
			xParse = parse(x);
		}
		if (yParse == null) {
			yParse = parse(y);
		}
	}

	protected XPathExpression parse(String function) throws XPathSyntaxException {
		if (function == null) {
			return null;
		}
		return XPathParseTool.parseXPath("string(" + function + ")");
	}
	
	public Double evaluateX(EvaluationContext context) throws XPathSyntaxException {
		parse();
		return evaluateExpression(xParse, context);
	}
	
	public Double evaluateY(EvaluationContext context) throws XPathSyntaxException {
		parse();
		return evaluateExpression(yParse, context);
	}
	
	protected Double evaluateExpression(XPathExpression expression, EvaluationContext context) {
		if (expression != null) {
			String value = (String) expression.eval(context.getMainInstance(), context);
			if (value.length() > 0) {
				return Double.valueOf(value);
			}
		}
		return null;
	}
}

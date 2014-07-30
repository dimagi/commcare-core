package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class BubbleSeries extends XYSeries {
	private String radius;
	private XPathExpression radiusParse;

	public BubbleSeries(String nodeSet) {
		super(nodeSet);
	}

	public String getRadius() {
		return radius;
	}
	
	public void setRadius(String radius) {
		this.radius = radius;
		this.radiusParse = null;
	}

	protected void parse() throws XPathSyntaxException {
		super.parse();
		if (radiusParse == null) {
			radiusParse = parse(radius);
		}
	}

	public Double evaluateRadius(EvaluationContext context) throws XPathSyntaxException {
		parse();
		return evaluateExpression(radiusParse, context);
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}
}

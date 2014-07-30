package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
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
		super.readExternal(in, pf);
		radius = ExtUtil.readString(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		super.writeExternal(out);
		ExtUtil.writeString(out, radius);
	}
}

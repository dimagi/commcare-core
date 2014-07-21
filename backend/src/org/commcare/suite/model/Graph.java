package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class Graph implements Externalizable, IDetailTemplate {
	private Vector<Series> series;
	
	public Graph() {
		series = new Vector<Series>();
	}
	
	public void addSeries(Series s) {
		series.addElement(s);
	}
	
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}

	public String evaluate(EvaluationContext context) {
		String csv = "";
		try {
			for (Series s : series) {
				XPathExpression xParse = XPathParseTool.parseXPath("string(" + s.getX() + ")");
				XPathExpression yParse = XPathParseTool.parseXPath("string(" + s.getY() + ")");
				
				Vector<TreeReference> refList = context.expandReference(s.getNodeSet());
				for (TreeReference ref : refList) {
					EvaluationContext temp = new EvaluationContext(context, ref);
					csv += (String)xParse.eval(temp.getMainInstance(), temp) + "," + (String)yParse.eval(temp.getMainInstance(), temp) + "&";
				}
				csv += "===";
			}
		}
		catch (XPathSyntaxException e) {
			e.printStackTrace();
		}
		return csv;
	}

}

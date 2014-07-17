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
		TreeReference xRef = series.elementAt(0).getX();
		TreeReference yRef = series.elementAt(0).getY();
		Vector<TreeReference> xList = context.expandReference(xRef);
		Vector<TreeReference> yList = context.expandReference(yRef);
		String csv = "";
		for (int i = 0; i < xList.size(); i++) {
			AbstractTreeElement xElement = context.resolveReference(xList.elementAt(i));
			AbstractTreeElement yElement = context.resolveReference(yList.elementAt(i));
			csv += "(" + Double.parseDouble(xElement.getValue().getDisplayText()) + "," + Double.parseDouble(yElement.getValue().getDisplayText()) + ")";
		}
		return csv;
	}

}

package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

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

public class GraphTemplate implements Externalizable, IDetailTemplate {
	private Vector<Series> series;
	
	public GraphTemplate() {
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

	public GraphData evaluate(EvaluationContext context) {
		GraphData graphData = new GraphData();
		try {
			for (Series s : series) {
				XPathExpression xParse = XPathParseTool.parseXPath("string(" + s.getX() + ")");
				XPathExpression yParse = XPathParseTool.parseXPath("string(" + s.getY() + ")");
				
				Vector<TreeReference> refList = context.expandReference(s.getNodeSet());
				SeriesData seriesData = new SeriesData();
				for (TreeReference ref : refList) {
					EvaluationContext refContext = new EvaluationContext(context, ref);
					String x = (String) xParse.eval(refContext.getMainInstance(), refContext);
					String y = (String) yParse.eval(refContext.getMainInstance(), refContext);
					if (x.length() > 0 && y.length() > 0) {
						seriesData.addPoint(new PointData(Double.valueOf(x), Double.valueOf(y)));
					}
				}
				graphData.addSeries(seriesData);
			}
		}
		catch (XPathSyntaxException e) {
			e.printStackTrace();
		}
		return graphData;
	}

}

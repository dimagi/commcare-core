package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.DetailTemplate;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * Defines a graph: type, set of series, set of text annotations, and key-value-based configuration.
 * @author jschweers
 */
public class Graph implements Externalizable, DetailTemplate, Configurable {
    public static final String TYPE_XY = "xy";
    public static final String TYPE_BUBBLE = "bubble";
    public static final String TYPE_TIME = "time";

    private String mType;
    private Vector<XYSeries> mSeries;
    private Hashtable<String, Text> mConfiguration;
    private Vector<Annotation> mAnnotations;
    
    public Graph() {
        mSeries = new Vector<XYSeries>();
        mConfiguration = new Hashtable<String, Text>();
        mAnnotations = new Vector<Annotation>();
    }
    
    public String getType() {
        return mType;
    }
    
    public void setType(String type) {
        mType = type;
    }
    
    public void addSeries(XYSeries s) {
        mSeries.addElement(s);
    }
    
    public void addAnnotation(Annotation a) {
        mAnnotations.addElement(a);
    }
    
    public Text getConfiguration(String key) {
        return mConfiguration.get(key);
    }
    
    public void setConfiguration(String key, Text value) {
        mConfiguration.put(key, value);
    }

    public Enumeration getConfigurationKeys() {
        return mConfiguration.keys();
    }
    
    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        ExtUtil.readString(in);
        mConfiguration = (Hashtable<String, Text>)ExtUtil.read(in, new ExtWrapMap(String.class, Text.class), pf);
        mSeries = (Vector<XYSeries>)ExtUtil.read(in, new ExtWrapList(XYSeries.class), pf);
        mAnnotations = (Vector<Annotation>)ExtUtil.read(in,  new ExtWrapList(Annotation.class), pf);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, mType);
        ExtUtil.write(out, new ExtWrapMap(mConfiguration));
        ExtUtil.write(out, new ExtWrapList(mSeries));
        ExtUtil.write(out, new ExtWrapList(mAnnotations));
    }

    /*
     * (non-Javadoc)
     * @see org.commcare.suite.model.DetailTemplate#evaluate(org.javarosa.core.model.condition.EvaluationContext)
     */
    public GraphData evaluate(EvaluationContext context) {
        GraphData data = new GraphData();
        data.setType(mType);
        evaluateSeries(data, context);
        evaluateAnnotations(data, context);
        evaluateConfiguration(this, data, context);
        return data;
    }
    
    /*
     * Helper for evaluate. Looks at annotations only.
     */
    private void evaluateAnnotations(GraphData graphData, EvaluationContext context) {
        for (Annotation a : mAnnotations) {
            graphData.addAnnotation(new AnnotationData(
                Double.valueOf(a.getX().evaluate(context)), 
                Double.valueOf(a.getY().evaluate(context)), 
                a.getAnnotation().evaluate(context)
            ));
        }
    }
    
    /*
     * Helper for evaluate. Looks at configuration only.
     */
    private void evaluateConfiguration(Configurable template, ConfigurableData data, EvaluationContext context) {
        Enumeration e = template.getConfigurationKeys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            data.setConfiguration(key, template.getConfiguration(key).evaluate(context));
        }
    }
    
    /*
     * Helper for evaluate. Looks at a single series.
     */
    private void evaluateSeries(GraphData graphData, EvaluationContext context) {
        try {
            for (XYSeries s : mSeries) {
                Vector<TreeReference> refList = context.expandReference(s.getNodeSet());
                SeriesData seriesData = new SeriesData();
                evaluateConfiguration(s, seriesData, context);
                for (TreeReference ref : refList) {
                    EvaluationContext refContext = new EvaluationContext(context, ref);
                    String x = s.evaluateX(refContext);
                    String y = s.evaluateY(refContext);
                    if (x != null && y != null) {
                        if (graphData.getType().equals(Graph.TYPE_BUBBLE)) {
                            Double radius = Double.valueOf(((BubbleSeries) s).evaluateRadius(refContext));
                            seriesData.addPoint(new BubblePointData(Double.valueOf(x), Double.valueOf(y), radius));
                        }
                        else if (graphData.getType().equals(Graph.TYPE_TIME)) {
                            Calendar c = Calendar.getInstance();
                            c.set(Calendar.YEAR, Integer.valueOf(x.substring(0, 4)));
                            c.set(Calendar.MONTH, Calendar.JANUARY + Integer.valueOf(x.substring(5, 7)) - 1);
                            c.set(Calendar.DATE, Integer.valueOf(x.substring(8)));
                            seriesData.addPoint(new TimePointData(c.getTime(), Double.valueOf(y)));
                        }
                        else {
                            seriesData.addPoint(new XYPointData(Double.valueOf(x), Double.valueOf(y)));
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

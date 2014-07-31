/**
 * Contains all of the fully-evaluated data to draw a graph: a type, set of series, set of text annotations, and key-value map of configuration.
 */
package org.commcare.suite.model.graph;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class GraphData implements ConfigurableData {
	private String mType;
	private Vector<SeriesData> mSeries;
	private Hashtable<String, String> mConfiguration;
	private Vector<AnnotationData> mAnnotations;

	public GraphData() {
		mSeries = new Vector<SeriesData>();
		mConfiguration = new Hashtable<String, String>();
		mAnnotations = new Vector<AnnotationData>();
	}
	
	public String getType() {
		return mType;
	}
	
	public void setType(String type) {
		mType = type;
	}

	public Iterator<SeriesData> getSeriesIterator() {
		return mSeries.iterator();
	}
	
	public void addSeries(SeriesData s) {
		mSeries.addElement(s);
	}
	
	public void addAnnotation(AnnotationData a) {
		mAnnotations.addElement(a);
	}
	
	public Iterator<AnnotationData> getAnnotationIterator() {
		return mAnnotations.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.commcare.suite.model.graph.ConfigurableData#setConfiguration(java.lang.String, java.lang.String)
	 */
	public void setConfiguration(String key, String value) {
		mConfiguration.put(key, value);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.commcare.suite.model.graph.ConfigurableData#getConfiguration(java.lang.String)
	 */
	public String getConfiguration(String key) {
		return mConfiguration.get(key);
	}
	
}

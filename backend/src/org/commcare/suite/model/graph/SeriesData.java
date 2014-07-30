package org.commcare.suite.model.graph;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class SeriesData implements ConfigurableData {
	private Vector<XYPointData> points;
	private Hashtable<String, String> configuration;

	public SeriesData() {
		points = new Vector<XYPointData>();
		configuration = new Hashtable<String, String>();
	}

	public void addPoint(XYPointData p) {
		points.addElement(p);
	}
	
	public Iterator<XYPointData> getPointsIterator() {
		return points.iterator();
	}
	
	public int size() {
		return points.size();
	}
	
	public void setConfiguration(String key, String value) {
		configuration.put(key, value);
	}
	
	public String getConfiguration(String key) {
		return configuration.get(key);
	}
}

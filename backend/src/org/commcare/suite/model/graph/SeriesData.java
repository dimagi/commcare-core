package org.commcare.suite.model.graph;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class SeriesData implements ConfigurableData {
	private Vector<PointData> points;
	private Hashtable<String, String> configuration;

	public SeriesData() {
		points = new Vector<PointData>();
		configuration = new Hashtable<String, String>();
	}

	public void addPoint(PointData p) {
		points.addElement(p);
	}
	
	public Iterator<PointData> getPointsIterator() {
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

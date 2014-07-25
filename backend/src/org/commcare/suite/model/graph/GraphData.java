package org.commcare.suite.model.graph;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class GraphData {
	private String type;
	private Vector<SeriesData> series;
	private Hashtable<String, String> configuration;

	public GraphData() {
		series = new Vector<SeriesData>();
		configuration = new Hashtable<String, String>();
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public Iterator<SeriesData> getSeriesIterator() {
		return series.iterator();
	}
	
	public void addSeries(SeriesData s) {
		series.addElement(s);
	}

	public void setConfiguration(String key, String value) {
		configuration.put(key, value);
	}
	
	public String getConfiguration(String key) {
		return configuration.get(key);
	}
	
}

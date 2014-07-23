package org.commcare.suite.model.graph;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class GraphData {
	Hashtable<String, String> configuration;
	Vector<SeriesData> series;

	public GraphData() {
		series = new Vector<SeriesData>();
		configuration = new Hashtable<String, String>();
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

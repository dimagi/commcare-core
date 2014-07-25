package org.commcare.suite.model.graph;

import java.util.Iterator;
import java.util.Vector;

public class SeriesData {
	private Vector<PointData> points;

	public SeriesData() {
		points = new Vector<PointData>();
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
}

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
	
	// TODO: perhaps the series should store this value, or have a BubbleSeriesData class derived from this
	public boolean hasRadius() {
		return points.size() > 0 && points.elementAt(0).getRadius() != null;
	}

	public int size() {
		return points.size();
	}
}

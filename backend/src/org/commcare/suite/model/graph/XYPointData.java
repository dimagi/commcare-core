package org.commcare.suite.model.graph;

/**
 * Representation of a point on an x, y plane.
 * @author jschweers
 */
public class XYPointData {
	private Double mX;
	private Double mY;

	public XYPointData(Double x, Double y) {
		mX = new Double(x);
		mY = new Double(y);
	}
	
	public Double getX() {
		return mX;
	}
	
	public Double getY() {
		return mY;
	}
	
}

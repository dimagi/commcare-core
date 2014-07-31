/**
 * Representation of a point on a bubble chart, which has an x, y position and an additional value for the bubble's radius.
 * @author jschweers
 */
package org.commcare.suite.model.graph;

public class BubblePointData extends XYPointData {
	private Double mRadius = null;

	public BubblePointData(Double x, Double y, Double radius) {
		super(x, y);
		mRadius = radius;
	}

	public Double getRadius() {
		return mRadius;
	}

}

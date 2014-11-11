package org.commcare.suite.model.graph;

/**
 * Representation of a point on a bubble chart, which has an x, y position and an additional value for the bubble's radius.
 * @author jschweers
 */
public class BubblePointData extends XYPointData {
    private Double mRadius = null;

    public BubblePointData(String x, String y, Double radius) {
        super(x, y);
        mRadius = radius;
    }

    public Double getRadius() {
        return mRadius;
    }

}

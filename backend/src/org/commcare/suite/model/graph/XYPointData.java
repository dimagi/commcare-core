package org.commcare.suite.model.graph;

/**
 * Representation of a point on an x, y plane.
 * @author jschweers
 */
public class XYPointData {
    private Double mX = null;
    private Double mY = null;

    public XYPointData(Double x, Double y) {
        if (x != null) {
            mX = new Double(x);
        }
        if (y != null) {
            mY = new Double(y);
        }
    }
    
    public Double getX() {
        return mX;
    }
    
    public Double getY() {
        return mY;
    }
    
}

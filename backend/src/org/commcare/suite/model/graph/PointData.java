package org.commcare.suite.model.graph;

public class PointData {
	//dummy
	private Double x;
	private Double y;
	private Double radius = null;	// bubble charts

	public PointData(Double x, Double y) {
		this.x = new Double(x);
		this.y = new Double(y);
	}
	
	public PointData(Double x, Double y, Double radius) {
		this.x = new Double(x);
		this.y = new Double(y);
		this.radius = new Double(radius);
	}
	
	public Double getX() {
		return x;
	}
	
	public Double getY() {
		return y;
	}
	
	public Double getRadius() {
		return radius;
	}

}

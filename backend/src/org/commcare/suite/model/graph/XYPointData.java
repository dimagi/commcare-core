package org.commcare.suite.model.graph;

public class XYPointData {
	private Double x;
	private Double y;

	public XYPointData(Double x, Double y) {
		this.x = new Double(x);
		this.y = new Double(y);
	}
	
	public Double getX() {
		return x;
	}
	
	public Double getY() {
		return y;
	}
	
}

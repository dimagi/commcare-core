package org.commcare.suite.model.graph;

public class PointData {
	private double x;
	private double y;

	public PointData() {
		this(0, 0);
	}
	
	public PointData(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}

}

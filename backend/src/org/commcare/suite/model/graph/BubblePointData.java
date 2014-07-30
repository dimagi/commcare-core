package org.commcare.suite.model.graph;

public class BubblePointData extends XYPointData {
	private Double radius = null;

	public BubblePointData(Double x, Double y, Double radius) {
		super(x, y);
		this.radius = radius;
	}

	public Double getRadius() {
		return radius;
	}

}

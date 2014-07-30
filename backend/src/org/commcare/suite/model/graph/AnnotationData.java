package org.commcare.suite.model.graph;

public class AnnotationData extends XYPointData {
	private String annotation;

	public AnnotationData(Double x, Double y, String annotation) {
		super(x, y);
		this.annotation = annotation;
	}

	public String getAnnotation() {
		return annotation;
	}
}

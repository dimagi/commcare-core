package org.commcare.suite.model;

public class Annotation {
	private Text x;
	private Text y;
	private Text annotation;

	public Annotation(Text x, Text y, Text annotation) {
		this.x = x;
		this.y = y;
		this.annotation = annotation;
	}

	public Text getX() {
		return x;
	}
	
	public Text getY() {
		return y;
	}
	
	public Text getAnnotation() {
		return annotation;
	}
}

/**
 * Data for an annotation, which is text drawn at a specified x, y coordinate on a graph.
 * @author jschweers
 */
package org.commcare.suite.model.graph;

public class AnnotationData extends XYPointData {
	private String mAnnotation;

	public AnnotationData(Double x, Double y, String annotation) {
		super(x, y);
		mAnnotation = annotation;
	}

	public String getAnnotation() {
		return mAnnotation;
	}
}

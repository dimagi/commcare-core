package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

public class Series implements Externalizable {
	private TreeReference nodeSet;
	
	// XPath expressions
	private String x;
	private String y;
	private String radius;	// bubble charts only

	public Series(String nodeSet, String x, String y, String radius) {
		this.nodeSet = XPathReference.getPathExpr(nodeSet).getReference(true);
		this.x = x;
		this.y = y;
		this.radius = radius;
	}
	
	public TreeReference getNodeSet() {
		return nodeSet;
	}
	
	public String getX() {
		return x;
	}
	
	public String getY() {
		return y;
	}
	
	public String getRadius() {
		return radius;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}

}

package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.commcare.suite.model.Text;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

public class Series implements Externalizable, Configurable {
	private TreeReference nodeSet;
	private Hashtable<String, Text> configuration;
	
	// XPath expressions
	private String x;
	private String y;
	private String radius;	// bubble charts only
	
	public Series(String nodeSet) {
		this.nodeSet = XPathReference.getPathExpr(nodeSet).getReference(true);
		configuration = new Hashtable<String, Text>();
	}
	
	public TreeReference getNodeSet() {
		return nodeSet;
	}
	
	public String getX() {
		return x;
	}
	
	public void setX(String x) {
		this.x = x;
	}
	
	public String getY() {
		return y;
	}
	
	public void setY(String y) {
		this.y = y;
	}
	
	public String getRadius() {
		return radius;
	}
	
	public void setRadius(String radius) {
		this.radius = radius;
	}

	public void setConfiguration(String key, Text value) {
		configuration.put(key, value);
	}
	
	public Text getConfiguration(String key) {
		return configuration.get(key);
	}

	public Enumeration getConfigurationKeys() {
		return configuration.keys();
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}

}

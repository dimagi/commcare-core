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
	private TreeReference x;	// XPath expression
	private TreeReference y;	// XPath expression

	public Series(String x, String y) {
		this.x = XPathReference.getPathExpr(x).getReference(true);
		this.y = XPathReference.getPathExpr(y).getReference(true);
	}
	
	public TreeReference getX() {
		return x;
	}
	
	public TreeReference getY() {
		return y;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		// TODO Auto-generated method stub

	}

	public void writeExternal(DataOutputStream out) throws IOException {
		// TODO Auto-generated method stub

	}

}

package org.commcare.suite.model.graph;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.commcare.suite.model.Text;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class Annotation implements Externalizable {
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

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		x = (Text)ExtUtil.read(in, Text.class, pf);
		y = (Text)ExtUtil.read(in,  Text.class, pf);
		annotation = (Text)ExtUtil.read(in, Text.class, pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, x);
		ExtUtil.write(out,  y);
		ExtUtil.write(out, annotation);
	}
}

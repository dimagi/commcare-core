/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Detail implements Externalizable {
	String id;
	
	FormInstance context;
	
	Vector<Text> headers;
	Vector<Text> templates;
	
	/**
	 * Serialization Only
	 */
	public Detail() {
		
	}
	
	public Detail(String id, FormInstance context, Vector<Text> headers, Vector<Text> templates) {
		this.id = id;
		this.context = context;
		this.headers = headers;
		this.templates = templates;
	}
	
	public String getId() {
		return id;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		context = (FormInstance)ExtUtil.read(in, FormInstance.class, pf);
		headers = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class), pf);
		templates = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class), pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out, context);
		ExtUtil.write(out, new ExtWrapList(headers));
		ExtUtil.write(out, new ExtWrapList(templates));
	}
	
}

/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Menu implements Externalizable {
	
	Text name;
	Vector<String> commandIds;
	String id;
	String root;
	
	/**
	 * Serialization only!!!
	 */
	public Menu() {
		
	}
	
	public Menu(String id, String root, Text name, Vector<String> commandIds) {
		this.id = id;
		this.root = root;
		this.name = name;
		this.commandIds = commandIds;
	}
	
	public String getRoot() {
		return root;
	}
	
	public Text getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public Vector<String> getCommandIds() {
		//UNSAFE! UNSAFE!
		return commandIds;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		root = ExtUtil.readString(in);
		name = (Text)ExtUtil.read(in, Text.class);
		commandIds = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class),pf);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out,ExtUtil.emptyIfNull(id));
		ExtUtil.writeString(out,root);
		ExtUtil.write(out,name);
		ExtUtil.write(out, new ExtWrapList(commandIds));
	}

}

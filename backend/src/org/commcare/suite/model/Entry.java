/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Entry implements Externalizable{
	private String xFormNamespace;
	private Hashtable<String, String> references;
	private String shortDetailId;
	private String longDetailId;
	private Text commandText;
	private String commandId;
	
	/**
	 * Serialization only!
	 */
	public Entry() {
		
	}
	
	public Entry(String commandId, Text commandText, String longDetailId,
			String shortDetailId, Hashtable<String, String> references,
			String formNamespace) {
		this.commandId = commandId  == null ? "" : commandId;;
		this.commandText = commandText;
		this.longDetailId = longDetailId == null ? "" : longDetailId;
		this.shortDetailId = shortDetailId  == null ? "" : shortDetailId;
		this.references = references;
		xFormNamespace = formNamespace;
	}
	
	public String getCommandId() {
		return commandId;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.xFormNamespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		this.shortDetailId = ExtUtil.readString(in);
		this.longDetailId = ExtUtil.readString(in);
		this.commandId = ExtUtil.readString(in);
		this.commandText = (Text)ExtUtil.read(in, Text.class, pf);
		
		ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(xFormNamespace));
		ExtUtil.writeString(out,shortDetailId);
		ExtUtil.writeString(out,longDetailId);
		ExtUtil.writeString(out,commandId);
		ExtUtil.write(out,commandText);
		
		ExtUtil.write(out, new ExtWrapMap(references));
	}
}

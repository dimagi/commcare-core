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
 * <p>An Entry definition describes a user 
 * initiated form entry action, what information
 * needs to be collected before that action can
 * begin, and what the User Interface should 
 * present to the user regarding these actions</p>
 * 
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
	
	/**
	 * @return the ID of this entry command. Used by Menus to determine
	 * where the command should be located.
	 */
	public String getCommandId() {
		return commandId;
	}
	
	/**
	 * @return A text whose evaluated string should be presented to the
	 * user as the entry point for this operation
	 */
	public Text getText() {
		return commandText;
	}
	
	/**
	 * @return A Key/Value set of references <refname, reftype> where
	 * reftype is one of <ul><li>case</li><li>reference</li></ul> defining
	 * a data type and refname is a string defining the name of that type
	 * in a data model. This set of references defines the data which needs
	 * to be available for the form entry action to begin.
	 */
	public Hashtable<String, String> getReferences() {
		return references;
	}
	
	/**
	 * @return The ID of a detail definition which should be used to 
	 * describe the data needed by the references to a user so that 
	 * they can select the appropriate datum to fulfil those references. 
	 */
	public String getShortDetailId() {
		return shortDetailId;
	}
	
	/**
	 * @return @return The ID of a detail definition which should be used to 
	 * describe an individual data object to a user.
	 */
	public String getLongDetailId() {
		return longDetailId;
	}
	
	/**
	 * @return The XForm Namespce of the form which should be filled out in
	 * the form entry session triggered by this action. null if no entry
	 * should occur [HACK]. 
	 */
	public String getXFormNamespace() {
		return xFormNamespace;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.xFormNamespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		this.shortDetailId = ExtUtil.readString(in);
		this.longDetailId = ExtUtil.readString(in);
		this.commandId = ExtUtil.readString(in);
		this.commandText = (Text)ExtUtil.read(in, Text.class, pf);
		
		references = (Hashtable<String,String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(xFormNamespace));
		ExtUtil.writeString(out,shortDetailId);
		ExtUtil.writeString(out,longDetailId);
		ExtUtil.writeString(out,commandId);
		ExtUtil.write(out,commandText);
		
		ExtUtil.write(out, new ExtWrapMap(references));
	}
}

/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
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
	Vector<SessionDatum> data;
	private Text commandText;
	private String commandId;
	private String imageResource;
	private String audioResource;
	
	/**
	 * Serialization only!
	 */
	public Entry() {
		
	}
	
	public Entry(String commandId, Text commandText, Vector<SessionDatum> data,
			String formNamespace, String imageResource, String audioResource) {
		this.commandId = commandId  == null ? "" : commandId;
		this.commandText = commandText;
		this.data = data;
		xFormNamespace = formNamespace;
		this.imageResource = imageResource == null ? "" : imageResource;
		this.audioResource = audioResource == null ? "" : audioResource;
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
	 * @return The XForm Namespce of the form which should be filled out in
	 * the form entry session triggered by this action. null if no entry
	 * should occur [HACK]. 
	 */
	public String getXFormNamespace() {
		return xFormNamespace;
	}
	
	/**
	 * @return the URI of an optionally specified image resource to be used in the
	 * view displaying all xform entries.
	 */
	public String getImageURI(){
		return imageResource;
	}
	
	/**
	 * @return the URI of an optional audio resource to be used in the view displaying all xform entries
	 */
	public String getAudioURI(){
		return audioResource;
	}
	
	public Vector<SessionDatum> getSessionDataReqs() {
		return data;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.xFormNamespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		this.commandId = ExtUtil.readString(in);
		this.commandText = (Text)ExtUtil.read(in, Text.class, pf);
		this.imageResource = ExtUtil.readString(in);
		this.audioResource = ExtUtil.readString(in);
		
		data = (Vector<SessionDatum>)ExtUtil.read(in, new ExtWrapList(SessionDatum.class), pf);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(xFormNamespace));
		ExtUtil.writeString(out,commandId);
		ExtUtil.write(out,commandText);
		ExtUtil.write(out, imageResource);
		ExtUtil.write(out, audioResource);
		ExtUtil.write(out, new ExtWrapList(data));
	}
}

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
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

/**
 * <p>A Menu definition describes the structure of how
 * actions should be provided to the user in a CommCare
 * application.</p>
 * 
 * @author ctsims
 *
 */
public class Menu implements Externalizable {
	
	Text name;
	Vector<String> commandIds;
	XPathExpression[] commandExprs;
	String id;
	String root;
	String imageReference;
	String audioReference;
	
	/**
	 * Serialization only!!!
	 */
	public Menu() {
		
	}
	
	
	public Menu(String id, String root, Text name, Vector<String> commandIds, XPathExpression[] commandExprs, String imageReference, String audioReference) {
		this.id = id;
		this.root = root;
		this.name = name;
		this.commandIds = commandIds;
		this.commandExprs = commandExprs;
		this.imageReference = imageReference;
		this.audioReference = audioReference;
	}
	
	/**
	 * @return The ID of what menu an option to navigate to
	 * this menu should be displayed in.
	 */
	public String getRoot() {
		return root;
	}
	
	/**
	 * @return A Text which should be displayed to the user as
	 * the action which will display this menu.
	 */
	public Text getName() {
		return name;
	}
	
	/**
	 * @return The ID of this menu. <p>If this value is "root"
	 * many CommCare applications will support displaying this
	 * menu's options at the app home screen</p> 
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @return The ID of what command actions should be available
	 * when viewing this menu.
	 */
	public Vector<String> getCommandIds() {
		//UNSAFE! UNSAFE!
		return commandIds;
	}
	
	public XPathExpression getRelevantCondition(int index) {
		return commandExprs[index];
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
		commandExprs = new XPathExpression[ExtUtil.readInt(in)];
		for(int i = 0 ; i < commandExprs.length; ++i) {
			if(ExtUtil.readBool(in)) {
				commandExprs[i] = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged());
			}
		}
		imageReference = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		audioReference = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out,ExtUtil.emptyIfNull(id));
		ExtUtil.writeString(out,root);
		ExtUtil.write(out,name);
		ExtUtil.write(out, new ExtWrapList(commandIds));
		ExtUtil.writeNumeric(out, commandExprs.length);
		for(int i = 0 ; i < commandExprs.length ; ++i) {
			if(commandExprs[i] == null) {
				ExtUtil.writeBool(out, false);
			} else{
				ExtUtil.writeBool(out, true);
				ExtUtil.write(out, new ExtWrapTagged(commandExprs[i]));
			}
		}
		ExtUtil.writeString(out,ExtUtil.emptyIfNull(imageReference));
		ExtUtil.writeString(out,ExtUtil.emptyIfNull(audioReference));
	}


	public String getImageURI() {
		return imageReference;
	}
	
	public String getAudioURI() {
		// TODO Auto-generated method stub
		return audioReference;
	}

}

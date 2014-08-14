/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.services.Logger;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * <p>A display unit element contains text and a set of potential image/audio
 * references for menus or other UI elements</p>
 * 
 * @author ctsims
 *
 */
public class DisplayUnit implements Externalizable {
	
	Text name;
	String imageReference;
	String audioReference;
	
	/**
	 * Serialization only!!!
	 */
	public DisplayUnit() {
		
	}
	
	
	public DisplayUnit(Text name, String imageReference, String audioReference) {
		this.name = name;
		this.imageReference = imageReference;
		this.audioReference = audioReference;
	}
	
	/**
	 * @return A Text which should be displayed to the user as
	 * the action which will display this menu.
	 */
	public Text getText() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		name = (Text)ExtUtil.read(in, Text.class, pf);
		imageReference = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		audioReference = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.write(out,name);
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

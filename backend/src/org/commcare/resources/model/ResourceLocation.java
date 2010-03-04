/**
 * 
 */
package org.commcare.resources.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.commcare.reference.ReferenceUtil;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class ResourceLocation implements Persistable {
	private int recordId = -1;
	private int authority;
	private String location;
	private boolean relative;
	
	public ResourceLocation() {
		//For serialization only;
	}
	
	public ResourceLocation(int authority, String location) {
		this.authority = authority;
		this.location = location;
		this.relative = ReferenceUtil.isRelative(location);
	}

	public int getID() {
		return recordId;
	}

	public void setID(int ID) {
		recordId = ID;
	}
	
	public int getAuthority() {
		return authority;
	}
	public String getLocation() {
		return location;
	}
	
	public boolean isRelative() {
		return relative;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.authority = ExtUtil.readInt(in);
		this.location = ExtUtil.readString(in);
		this.relative = ReferenceUtil.isRelative(location);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, authority);
		ExtUtil.writeString(out, location);
		this.relative = ReferenceUtil.isRelative(location);
	}
	
	
	
}

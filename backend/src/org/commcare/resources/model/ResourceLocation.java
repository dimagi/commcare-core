/**
 * 
 */
package org.commcare.resources.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.commcare.reference.ReferenceManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class ResourceLocation implements Externalizable {
	private int authority;
	private String location;
	private boolean relative;
	
	public ResourceLocation() {
		//For serialization only;
	}
	
	public ResourceLocation(int authority, String location) {
		this.authority = authority;
		this.location = location;
		this.relative = ReferenceManager.isRelative(location);
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
		this.relative = ReferenceManager.isRelative(location);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, authority);
		ExtUtil.writeString(out, location);
		this.relative = ReferenceManager.isRelative(location);
	}
	
	
	
}

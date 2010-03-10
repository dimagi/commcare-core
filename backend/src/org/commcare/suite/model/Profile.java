package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * There should really probably only be one of these around at a time. Maybe
 * the RMS/Installer/etc should have a way to do that?
 * 
 * @author ctsims
 *
 */
public class Profile implements Persistable {
	
	public static final String STORAGE_KEY = "PROFILE";
	
	int recordId = -1;
	int version;
	String authRef;
	Vector<PropertySetter> properties;
	
	/**
	 * Serialization Only!
	 */
	public Profile() {
		
	}
	
	public Profile(int version, String authRef) {
		this.version = version;
		this.authRef = authRef;
	}
	
	public int getID() {
		return recordId;
	}

	public void setID(int ID) {
		recordId = ID;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String getAuthReference() {
		return authRef;
	}
	
	public void addPropertySetter(String key, String value) {
		this.addPropertySetter(key,value,false);
	}
	
	public void addPropertySetter(String key, String value, boolean force) {
		properties.addElement(new PropertySetter(key,value,force));
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean initializeProperties() {
		for(PropertySetter setter : properties) {
			String property = PropertyManager._().getSingularProperty(setter.getKey());
			if(property == null || setter.force) {
				PropertyManager._().setProperty(setter.getKey(), setter.getValue());
			}
		}
		this.properties.removeAllElements();
		return true;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		recordId = ExtUtil.readInt(in);
		version = ExtUtil.readInt(in);
		authRef = ExtUtil.readString(in);
		
		properties = (Vector<PropertySetter>)ExtUtil.read(in, new ExtWrapList(PropertySetter.class),pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeNumeric(out, version);
		ExtUtil.writeString(out, authRef);
		
		ExtUtil.write(out, new ExtWrapList(properties));
	}

	
	
	/**
	 * This is just a tiny little struct to make it reasonable to maintain 
	 * the properties until they are installed. 
	 * 
	 * @author ctsims
	 *
	 */
	private class PropertySetter implements Externalizable {
		String key;
		String value;
		boolean force;
		
		public String getKey() { return key; }
		public String getValue() { return value; }
		public boolean isForce() { return force; }

		/**
		 * Serialization Only!!!
		 */
		public PropertySetter() {
			
		}
		
		public PropertySetter(String key, String value, boolean force) {
			this.key = key;
			this.value = value;
			this.force = force;
		}

		public void readExternal(DataInputStream in, PrototypeFactory pf)
				throws IOException, DeserializationException {
			key = ExtUtil.readString(in);
			value = ExtUtil.readString(in);
			force = ExtUtil.readBool(in);
		}

		public void writeExternal(DataOutputStream out) throws IOException {
			ExtUtil.writeString(out,key);
			ExtUtil.writeString(out,value);
			ExtUtil.writeBool(out, force);
		}
		
	}
}

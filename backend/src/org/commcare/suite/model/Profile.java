package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.reference.Root;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
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
	
	public static final String FEATURE_REVIEW = "checkoff";
	
	int recordId = -1;
	int version;
	String authRef;
	Vector<PropertySetter> properties;
	Vector<Root> roots;
	
	Hashtable<String,Boolean> featureStatus;
	
	/**
	 * Serialization Only!
	 */
	public Profile() {
		
	}
	
	public Profile(int version, String authRef) {
		this.version = version;
		this.authRef = authRef;
		properties = new Vector<PropertySetter>();
		roots = new Vector<Root>();
		featureStatus = new Hashtable<String, Boolean>();
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
	
	public void addRoot(Root r) {
		this.roots.addElement(r);
	}
	
	public void addPropertySetter(String key, String value) {
		this.addPropertySetter(key,value,false);
	}
	
	public void addPropertySetter(String key, String value, boolean force) {
		properties.addElement(new PropertySetter(key,value,force));
	}
	
	public void setFeatureActive(String feature, boolean active) {
		this.featureStatus.put(feature, new Boolean(active));
	}
	
	public boolean isFeatureActive(String feature) {
		return featureStatus.get(feature).booleanValue();
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
		roots = (Vector<Root>)ExtUtil.read(in, new ExtWrapList(Root.class),pf);
		featureStatus = (Hashtable<String, Boolean>)ExtUtil.read(in, new ExtWrapMap(String.class, Boolean.class),pf);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeNumeric(out, version);
		ExtUtil.writeString(out, authRef);
		
		ExtUtil.write(out, new ExtWrapList(properties));
		ExtUtil.write(out, new ExtWrapList(roots));
		ExtUtil.write(out, new ExtWrapMap(featureStatus));
	}
}

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

public class Filter implements Externalizable {
	private static Filter empty;
	
	String raw;
	Hashtable<String,String> custom;
	
	
	public static Filter EmptyFilter() {
		if(empty == null) {
		   empty = new Filter();
		   empty.raw = "";
		   empty.custom = new Hashtable<String,String>();
		}
		return empty;
	}
	
	/**
	 * Note: For Serialization Only!!!
	 */
	public Filter() {
		
	}
	
	public Filter(String raw) {
		
	}
	
	public boolean isEmpty() {
		return raw.equals("") && custom.size() ==0;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		raw = ExtUtil.readString(in);
		custom = (Hashtable<String,String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
	}
	
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, raw);
		ExtUtil.write(out, new ExtWrapMap(custom));
	}

}

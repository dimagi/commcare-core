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
 * <p>
 * Filter definitions provide the relevant fields on which
 * data should be filtered for display in detail views.
 * 
 * This part of the system is still a work in progress as the
 * nature of what models are included strongly typed continues
 * to evolve. For now, this is mostly a placeholder which is
 * assumed to hold magic strings.
 *  
 * </p>
 * 
 * @author ctsims
 */
public class Filter implements Externalizable {
	private static Filter empty;
	
	String raw;
	Hashtable<String,String> custom;
	
	/**
	 * @return A filter representing the Empty filter, which
	 * provides no additional rules for what data should
	 * be filtered. This should always be used rather than
	 * a constructor for empty filters.
	 */
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
	
	/**
	 * Whether or not this filter doesn't provide any additional rules
	 * for what data should be filtered. No filter reference should
	 * ever be null, an empty filter reference (produced with the 
	 * EmptyFilter() static method) should be present instead.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return raw.equals("") && custom.size() ==0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		raw = ExtUtil.readString(in);
		custom = (Hashtable<String,String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, raw);
		ExtUtil.write(out, new ExtWrapMap(custom));
	}

}

package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
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
	
	public static final String FILTER_CASE = "case";
	public static final String FILTER_REFERRAL = "referral";
	
	public static final String TYPE = "fct";
	public static final String REFERRAL_TYPE = "frt";
	public static final String SHOW_CLOSED = "fsc";
	public static final String SHOW_RESOLVED = "fsrr";
	public static final String USER_PROTECTED = "fup";
	
	private static final String TRUE = new Boolean(true).toString(); 
	
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
		   empty = new Filter(null);
		}
		return empty;
	}
	
	/**
	 * @return A filter representing the Empty filter, which
	 * provides no additional rules for what data should
	 * be filtered. This should always be used rather than
	 * a constructor for empty filters.
	 */
	public static Filter RawFilter(String raw) {
		Filter rawFilter = new Filter(raw);
		
		return rawFilter;
	}
	
	/**
	 * Creates a Filter for referral Entities
	 * 
	 * @param referralType The referral type for filtered entities
	 * @param viewClosed True if resolve referrals shouldn't be filtered out
	 * 
	 * @return A Filter which can be used to filter referral objects
	 */
	public static Filter ReferralFilter(String referralType, boolean viewClosed) {
		Filter referralFilter = new Filter();
		Hashtable<String,String> data = new Hashtable<String,String>();
		data.put(FILTER_REFERRAL, TRUE);
		
		if(referralType != null) {
			data.put(REFERRAL_TYPE, referralType);
		}
		
		//Don't bother to put in the default value, it's assumed
		if(viewClosed != false) {
			data.put(SHOW_RESOLVED, new Boolean(true).toString());
		}
		
		referralFilter.custom = data;
		
		return referralFilter;
	}
	
	/**
	 * Creates a Filter for Case Entities
	 * 
	 * @param caseType The case type that should be used.
	 * 
	 * @return A Filter which can be used to filter case objects
	 */
	public static Filter CaseFilter(String caseType) {
		return CaseFilter(caseType, true);
	}
	
	/**
	 * Creates a Filter for Case Entities
	 * 
	 * @param caseType The case type that should be used.
	 * @param viewClosed True if "closed" cases shouldn't be filtered out
	 * 
	 * @return A Filter which can be used to filter case objects
	 */
	public static Filter CaseFilter(String caseType, boolean viewClosed) {
		Filter caseFilter = new Filter(null);
		Hashtable<String,String> data = new Hashtable<String,String>();
		data.put(FILTER_CASE, TRUE);
		
		if(caseType != null) {
			data.put(TYPE, caseType);
		}
		//Don't bother to put in the default value, it's assumed
		if(viewClosed != false) {
			data.put(SHOW_CLOSED, new Boolean(true).toString());
		}
		
		caseFilter.custom = data;
		
		return caseFilter;
	}
	
	public Filter merge(Filter other) {
		
		//Ugh, awful...
		String raw = null;
		if(this.raw != null && other.raw == null) {
			raw = this.raw;
		} else if(this.raw == null && other.raw != null) {
			raw = other.raw;
		} else if(this.raw != null && other.raw != null) {
			raw = this.raw + " AND " + other.raw;
		}
		
		Filter merged = new Filter(raw);
		mergeInto(merged.custom, this.custom);
		mergeInto(merged.custom, other.custom);
		
		return merged;
	}
	
	private void mergeInto(Hashtable destination, Hashtable source) {
		for(Enumeration en = source.keys() ; en.hasMoreElements();) {
			Object key = en.nextElement();
			Object value = source.get(key);
			destination.put(key, value);
		}
	}
	
	/**
	 * Note: For Serialization Only!!!
	 */
	public Filter() {
		
	}
	
	public Filter(String raw) {
		custom = new Hashtable<String,String>();
		this.raw = raw;
	}
	
	public String getParam(String parameter) {
		if(custom.containsKey(parameter)) {
			return custom.get(parameter);
		} else {
			return null;
		}
	}
	
	public boolean paramSet(String parameter) {
		return custom.containsKey(parameter);
	}
	
	public String getRaw() {
		return raw;
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
		return raw == null && custom.size() ==0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		raw = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		custom = (Hashtable<String,String>)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(raw));
		ExtUtil.write(out, new ExtWrapMap(custom));
	}

}

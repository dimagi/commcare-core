/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class SessionDatum implements Externalizable {
	
	private String id;
	private String nodeset;
	private String shortDetail; 
	private String longDetail; 
	private String value;
	
	private int type;
	
	public static final int DATUM_TYPE_NORMAL = 0;
	public static final int DATUM_TYPE_FORM = 1;
	
	public SessionDatum() {
		
	}

	public SessionDatum(String id, String nodeset, String shortDetail, String longDetail, String value) {
		type = DATUM_TYPE_NORMAL;
		//TODO: Nodeset should be an xpathpath expression, probably?
		this.id = id;
		this.nodeset = nodeset;
		this.shortDetail = shortDetail;
		this.longDetail = longDetail;
		this.value = value;
	}
	
	public static SessionDatum FormIdDatum(String calculate) {
		SessionDatum ret = new SessionDatum();
		ret.id = "";
		ret.type = DATUM_TYPE_FORM;
		ret.value = calculate;
		return ret;
	}
	
	public String getDataId() {
		return id;
	}
	
	public String getNodeset() {
		return nodeset;
	}

	/**
	 * @return the shortDetail
	 */
	public String getShortDetail() {
		return shortDetail;
	}

	/**
	 * @return the longDetail
	 */
	public String getLongDetail() {
		return longDetail;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	
	public int getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		id = ExtUtil.readString(in);
		type = ExtUtil.readInt(in);
		
		//TODO: Parse this stuff differently based on type?
		nodeset = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		shortDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		longDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, id);
		ExtUtil.writeNumeric(out, type);
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(nodeset));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(shortDetail));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(longDetail));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
	}

}

/**
 * 
 */
package org.commcare.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class PeriodicEventRecord implements Persistable, IMetaData {
	
	public static final String STORAGE_KEY = "periodic_e_r";
	
	public static final String META_EVENT_KEY = "event_key";
	
	private int recordId = -1;
		
	private Date nextOccurance;
	
	private Date lastOccurance;
	
	private String key;
	
	/**
	 * DO NOT CALL. FOR SERIALIZATION ONLY
	 */
	public PeriodicEventRecord() {
		//FOR SERIALIZATION ONLY!!!
	}
	
	protected PeriodicEventRecord(String key, Date nextOccurance) {
		this.key = key;
		this.lastOccurance = new Date(0);
		this.nextOccurance = nextOccurance;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.Persistable#setID(int)
	 */
	public void setID(int ID) {
		this.recordId = ID;

	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.services.storage.Persistable#getID()
	 */
	public int getID() {
		// TODO Auto-generated method stub
		return recordId;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		recordId = ExtUtil.readInt(in);
		nextOccurance = ExtUtil.readDate(in);
		lastOccurance = ExtUtil.readDate(in);
		key = ExtUtil.readString(in);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.write(out, nextOccurance);
		ExtUtil.write(out, lastOccurance);
		ExtUtil.write(out, key);
	}

	public Hashtable getMetaData() {
		Hashtable ret = new Hashtable();
		
		for(String field : getMetaDataFields()) {
			ret.put(field, getMetaData(field));
		}
		return ret;
	}

	public String[] getMetaDataFields() {
		return new String[] {META_EVENT_KEY};
	}

	public Object getMetaData(String fieldName) {
		if(META_EVENT_KEY.equals(fieldName)) {
			return key;
		} else {
			throw new IllegalArgumentException("No metadata field " + fieldName  + " in the periodic event storage system");
		}
	}

	public Date getNextTrigger() {
		return nextOccurance;
	}

	public String getEventKey() {
		return key;
	}

	public void setLastOccurance(Date date) {
		this.lastOccurance = date;
	}

	public void setNextScheduledDate(Date nextSchedule) {
		this.nextOccurance = nextSchedule;
	}

	public Date getLastOccurance() {
		return lastOccurance;
	}
}

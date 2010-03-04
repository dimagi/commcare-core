/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import org.commcare.resources.model.ResourceTable;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Suite implements Persistable {
	
	public static final String STORAGE_KEY = "SUITE";
	
	private int version;
	int recordId = -1;
	
	private Hashtable<String, Detail> details;
	private Hashtable<String, Entry> entries;
	
	/**
	 * For serialization only;
	 */
	public Suite() {
		
	}
	
	public Suite(int version, Hashtable<String, Detail> details, Hashtable<String, Entry> entries) {
		this.version = version;
		this.details = details;
		this.entries = entries;
	}

	public int getID() {
		return recordId;
	}

	public void setID(int ID) {
		recordId = ID;
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.recordId = ExtUtil.readInt(in);
		this.version = ExtUtil.readInt(in);
		this.details = (Hashtable<String, Detail>)ExtUtil.read(in,new ExtWrapMap(String.class, Detail.class), pf);
		this.entries = (Hashtable<String, Entry>)ExtUtil.read(in,new ExtWrapMap(String.class, Entry.class), pf);
		
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeNumeric(out, version);
		ExtUtil.write(out, new ExtWrapMap(details));
		ExtUtil.write(out, new ExtWrapMap(entries));
	}
	
	
}

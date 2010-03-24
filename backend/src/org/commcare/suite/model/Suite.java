/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * <p>Suites are the central point of   
 * @author ctsims
 *
 */
public class Suite implements Persistable {
	
	public static final String STORAGE_KEY = "SUITE";
	
	private int version;
	int recordId = -1;
	
	/** String(detail id) -> Detail Object **/
	private Hashtable<String, Detail> details;
	
	/** String(Entry id (also the same for menus) ) -> Entry Object **/
	private Hashtable<String, Entry> entries;
	private Vector<Menu> menus;
	
	/**
	 * For serialization only;
	 */
	public Suite() {
		
	}
	
	public Suite(int version, Hashtable<String, Detail> details, Hashtable<String, Entry> entries, Vector<Menu> menus) {
		this.version = version;
		this.details = details;
		this.entries = entries;
		this.menus = menus;
	}

	public int getID() {
		return recordId;
	}

	public void setID(int ID) {
		recordId = ID;
	}
	
	public String getName() {
		//BAD! BAD! BAD!
		return "Suite " + this.recordId;
	}
	
	public Vector<Menu> getMenus() {
		return menus;
	}
	
	/**
	 * WOAH! UNSAFE! Copy, maybe? But this is _wicked_ dangerous.
	 * 
	 * @return
	 */
	public Hashtable<String, Entry> getEntries() {
		return entries;
	}
	
	public Detail getDetail(String id) {
		return details.get(id);
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.recordId = ExtUtil.readInt(in);
		this.version = ExtUtil.readInt(in);
		this.details = (Hashtable<String, Detail>)ExtUtil.read(in,new ExtWrapMap(String.class, Detail.class), pf);
		this.entries = (Hashtable<String, Entry>)ExtUtil.read(in,new ExtWrapMap(String.class, Entry.class), pf);
		this.menus = (Vector<Menu>)ExtUtil.read(in, new ExtWrapList(Menu.class),pf);
		
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeNumeric(out, version);
		ExtUtil.write(out, new ExtWrapMap(details));
		ExtUtil.write(out, new ExtWrapMap(entries));
		ExtUtil.write(out, new ExtWrapList(menus));
	}
	
	
}

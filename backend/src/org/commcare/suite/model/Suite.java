/**
 *
 */
package org.commcare.suite.model;

import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * <p>Suites are containers for a set of actions,
 * detail definitions, and UI information. A suite
 * generally contains a set of form entry actions
 * related to the same case ID, sometimes including
 * referrals.</p>
 *
 * @author ctsims
 */
public class Suite implements Persistable {

    public static final String STORAGE_KEY = "SUITE";

    private int version;
    int recordId = -1;

    /**
     * String(detail id) -> Detail Object *
     */
    private Hashtable<String, Detail> details;

    /**
     * String(Entry id (also the same for menus) ) -> Entry Object *
     */
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

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.storage.Persistable#getID()
     */
    public int getID() {
        return recordId;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.storage.Persistable#setID(int)
     */
    public void setID(int ID) {
        recordId = ID;
    }

    /**
     * @return The menus which define how to access the actions
     * which are available in this suite.
     */
    public Vector<Menu> getMenus() {
        return menus;
    }

    /**
     * WOAH! UNSAFE! Copy, maybe? But this is _wicked_ dangerous.
     *
     * @return The set of entry actions which are defined by this
     * suite, indexed by their id (which is present in the menu
     * definitions).
     */
    public Hashtable<String, Entry> getEntries() {
        return entries;
    }

    /**
     * @param id The String ID of a detail definition
     * @return A Detail definition associated with the provided
     * id.
     */
    public Detail getDetail(String id) {
        return details.get(id);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.recordId = ExtUtil.readInt(in);
        this.version = ExtUtil.readInt(in);
        this.details = (Hashtable<String, Detail>)ExtUtil.read(in, new ExtWrapMap(String.class, Detail.class), pf);
        this.entries = (Hashtable<String, Entry>)ExtUtil.read(in, new ExtWrapMap(String.class, Entry.class), pf);
        this.menus = (Vector<Menu>)ExtUtil.read(in, new ExtWrapList(Menu.class), pf);

    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeNumeric(out, version);
        ExtUtil.write(out, new ExtWrapMap(details));
        ExtUtil.write(out, new ExtWrapMap(entries));
        ExtUtil.write(out, new ExtWrapList(menus));
    }


}

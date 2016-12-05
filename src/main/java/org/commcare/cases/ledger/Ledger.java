package org.commcare.cases.ledger;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A Ledger is a data model which tracks numeric data organized into
 * different sections with different meanings.
 *
 * @author ctsims
 */
public class Ledger implements Persistable, IMetaData {

    //NOTE: Right now this is (lazily) implemented assuming that each ledger
    //object tracks _all_ of the sections for an entity, which will likely be a terrible way
    //to do things long-term.


    public static final String STORAGE_KEY = "ledger";
    public static final String INDEX_ENTITY_ID = "entity-id";

    String entityId;
    int recordId = -1;
    Hashtable<String, Hashtable<String, Integer>> sections;

    public Ledger() {

    }

    public Ledger(String entityId) {
        this.entityId = entityId;
        this.sections = new Hashtable<>();
    }

    /**
     * Get the ID of the linked entity associated with this Ledger record
     */
    public String getEntiyId() {
        return entityId;
    }

    /**
     * Retrieve an entry from a specific section of the ledger.
     *
     * If no entry is defined, the ledger will return the value '0'
     *
     * @param sectionId The section containing the entry
     * @param entryId   The Id of the entry to retrieve
     * @return the entry value. '0' if no entry exists.
     */
    public int getEntry(String sectionId, String entryId) {
        if (!sections.containsKey(sectionId) || !sections.get(sectionId).containsKey(entryId)) {
            return 0;
        }
        return sections.get(sectionId).get(entryId);
    }

    /**
     * @return The list of sections available in this ledger
     */
    public String[] getSectionList() {
        String[] sectionList = new String[sections.size()];
        int i = 0;
        for (Enumeration e = sections.keys(); e.hasMoreElements(); ) {
            sectionList[i] = (String)e.nextElement();
            ++i;
        }
        return sectionList;

    }

    /**
     * Retrieves a list of all entries (by ID) defined in a
     * section of the ledger
     *
     * @param sectionId The ID of a section
     * @return The IDs of all entries defined in the provided section
     */
    public String[] getListOfEntries(String sectionId) {
        Hashtable<String, Integer> entries = sections.get(sectionId);
        String[] entryList = new String[entries.size()];
        int i = 0;
        for (Enumeration e = entries.keys(); e.hasMoreElements(); ) {
            entryList[i] = (String)e.nextElement();
            ++i;
        }
        return entryList;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        recordId = ExtUtil.readInt(in);
        entityId = ExtUtil.readString(in);
        sections = (Hashtable<String, Hashtable<String, Integer>>)ExtUtil.read(in, new ExtWrapMap(String.class, new ExtWrapMap(String.class, Integer.class)), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, entityId);
        ExtUtil.write(out, new ExtWrapMap(sections, new ExtWrapMap(String.class, Integer.class)));
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    @Override
    public int getID() {
        return recordId;
    }

    /**
     * Sets the value of an entry in the specified section of this ledger
     */
    public void setEntry(String sectionId, String entryId, int quantity) {
        if (!sections.containsKey(sectionId)) {
            sections.put(sectionId, new Hashtable<String, Integer>());
        }
        sections.get(sectionId).put(entryId, new Integer(quantity));
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{INDEX_ENTITY_ID};
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (fieldName.equals(INDEX_ENTITY_ID)) {
            return entityId;
        } else {
            throw new IllegalArgumentException("No metadata field " + fieldName + " in the ledger storage system");
        }
    }
}

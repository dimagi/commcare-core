package org.commcare.model;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

/**
 * A periodic event record is a database model for maintaining the
 * execution scheduling of periodic events. There should be one or
 * zero records for each type of periodic event, maintaining a record
 * of the next and previous occurrences of that event.
 *
 * TODO: Add general functionality for maintaining other state for
 * these records: Disabled, etc.
 *
 * @author ctsims
 *
 */
public class PeriodicEventRecord implements Persistable, IMetaData {

    public static final String STORAGE_KEY = "periodic_e_r";

    /** The type of event that this record is maintaining */
    public static final String META_EVENT_KEY = "event_key";

    private int recordId = -1;

    private Date nextOccurance;

    private Date lastOccurance;

    private String key;

    /**
     * DO NOT CALL. FOR SERIALIZATION ONLY.
     *
     * Records will be created by the scheduler.
     */
    public PeriodicEventRecord() {
        //FOR SERIALIZATION ONLY!!!
    }

    /**
     * Creates a record for a periodic event. Should only be called by the
     * package.
     *
     * @param key
     * @param nextOccurance
     */
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

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.storage.IMetaData#getMetaData()
     */
    public String[] getMetaDataFields() {
        return new String[] {META_EVENT_KEY};
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.services.storage.IMetaData#getMetaData()
     */
    public Object getMetaData(String fieldName) {
        if(META_EVENT_KEY.equals(fieldName)) {
            return key;
        } else {
            throw new IllegalArgumentException("No metadata field " + fieldName  + " in the periodic event storage system");
        }
    }

    /**
     * @return The next scheduled time for this event to occur.
     */
    public Date getNextTrigger() {
        return nextOccurance;
    }

    /**
     * @return The unique event key tied to this record.
     */
    public String getEventKey() {
        return key;
    }

    /**
     * @param date The time this event last occurred.
     */
    public void setLastOccurance(Date date) {
        this.lastOccurance = date;
    }

    /**
     * @param nextSchedule The next time this event should be triggered.
     */
    public void setNextScheduledDate(Date nextSchedule) {
        this.nextOccurance = nextSchedule;
    }

    /**
     * @return The last time this event occurred
     */
    public Date getLastOccurance() {
        return lastOccurance;
    }
}

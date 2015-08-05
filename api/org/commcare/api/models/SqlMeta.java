package org.commcare.api.models;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Persistable object for tracking meta-data about a user's
 * sandbox status
 *
 * Created by wpride1 on 7/20/15.
 */
public class SqlMeta implements Persistable, IMetaData {

    public static final String STORAGE_KEY = "SQL_META";
    public static final String META_LAST_SYNC = "LAST_SYNC";

    private Date lastSync;
    private int metaId = -1;

    public SqlMeta(){
        this.lastSync = new Date();
        this.setID(-1);
    }

    @Override
    public void setID(int ID) {
        this.metaId = ID;
    }

    @Override
    public int getID() {
        return metaId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.lastSync = ExtUtil.readDate(in);
        this.metaId = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeDate(out, lastSync);
        ExtUtil.writeNumeric(out, metaId);
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[] {META_LAST_SYNC};
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (META_LAST_SYNC.equals(fieldName)) {
            return lastSync;
        }
        return null;
    }

    public void setLastSync(){
        this.lastSync = new Date();
    }
}

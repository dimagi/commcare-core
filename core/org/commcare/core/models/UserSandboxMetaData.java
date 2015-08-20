package org.commcare.core.models;

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
 * Persistable object for tracking information about the users sandbox. Stores
 * last sync date at the moment, will be expanded to include versioning
 * and sync token information
 *
 * Created by wpride1 on 7/20/15.
 */
public class UserSandboxMetaData implements Persistable, IMetaData {

    public static final String META_LAST_SYNC = "LAST_SYNC";

    private Date lastSync;
    private int metaId = -1;

    public UserSandboxMetaData(){
        this.lastSync = new Date();
        this.setID(-1);
    }


    public void setID(int ID) {
        this.metaId = ID;
    }


    public int getID() {
        return metaId;
    }


    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.lastSync = ExtUtil.readDate(in);
        this.metaId = ExtUtil.readInt(in);
    }


    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeDate(out, lastSync);
        ExtUtil.writeNumeric(out, metaId);
    }


    public String[] getMetaDataFields() {
        return new String[] {META_LAST_SYNC};
    }


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

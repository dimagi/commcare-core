package org.commcare.suite.model;

import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class UserRestore implements Persistable {
    public static final String STORAGE_KEY = "UserRestore";
    private int recordId = -1;
    private String restore;

    public UserRestore() {
    }

    public static UserRestore buildInMemoryUserRestore(InputStream restoreStream) throws IOException {
        UserRestore userRestore = new UserRestore();
        userRestore.restore = new String(StreamsUtil.inputStreamToByteArray(restoreStream));

        return userRestore;
    }

    public InputStream getRestoreStream() {
        try {
            return new ByteArrayInputStream(restore.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.recordId = ExtUtil.readInt(in);
        this.restore = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, restore);
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    @Override
    public int getID() {
        return recordId;
    }
}

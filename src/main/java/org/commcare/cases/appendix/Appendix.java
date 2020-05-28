package org.commcare.cases.appendix;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * An appendix tracks additional information about cases which can
 * be used to optimize how they are used
 *
 * @author ctsims
 */
public class Appendix implements Persistable, IMetaData {

    public static final String STORAGE_KEY = "appendix";

    public static final String META_STATE = "state";
    public static final String META_ID = "id";
    public static final String META_HASH = "hash";

    public static final String STATE_RECEIVED = "received";
    public static final String STATE_ACTIVE = "active";
    public static final String STATE_UNSTAGED = "unstaged";

    Vector<String> caseIndexes;

    private String appendixId;
    private String state;
    private String hash;

    private int recordId = -1;

    public Appendix() {

    }

    public Appendix(String appendixId, String hash, Vector<String> caseIndexes) {
        this.appendixId = appendixId;
        this.hash = hash;
        this.caseIndexes = caseIndexes;
        this.state = STATE_RECEIVED;
    }

    @Override
    public void setID(int ID) {
        recordId = ID;
    }

    @Override
    public int getID() {
        return recordId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        recordId = (int)ExtUtil.readNumeric(in);
        appendixId = ExtUtil.readString(in);
        hash = ExtUtil.readString(in);
        state = ExtUtil.readString(in);

        caseIndexes = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, appendixId);
        ExtUtil.writeString(out, hash);
        ExtUtil.writeString(out, state);

        ExtUtil.write(out, new ExtWrapList(caseIndexes));
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{META_STATE, META_ID, META_HASH};
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (fieldName.equals(META_STATE)) {
            return state;
        } else if (fieldName.equals(META_ID)) {
            return appendixId;
        } else if (fieldName.equals(META_HASH)) {
            return hash;
        }
        return null;
    }

    public static Appendix getAppendix(
            IStorageUtilityIndexed<Appendix> storage, String id, String hash) {

        String[] ids = new String[] {META_ID, META_HASH};
        String[] values = new String[] {id, hash};

        List<Integer> recordIds = storage.getIDsForValues(ids, values);

        if (recordIds.size() > 1) {
            throw new RuntimeException(
                    "Appendix table should not contain more than one id/hash pair");
        } else if (recordIds.size() ==1) {
            return storage.read(recordIds.get(0));
        } else {
            return null;
        }
    }
}

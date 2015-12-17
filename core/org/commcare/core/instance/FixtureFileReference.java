package org.commcare.core.instance;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FixtureFileReference implements Persistable, IMetaData {
    public static final String STORAGE_KEY = "FIXTURE_DATA";
    public static final String META_ID = "fixture_id";

    private int recordId = -1;
    private String instanceId;
    private String filePath;

    public FixtureFileReference() {
        // for externalization
    }

    public FixtureFileReference(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Serialize fixture instance and save, encrypted, to file. Store filepath in fixture database
     *
     * @param fixtureInstance fixture form instance to save
     * @return serialization and save operation was successful.
     */
    public boolean writeFixture(FormInstance fixtureInstance) {
        return false;
    }

    public FormInstance readFixture() {
        return null;
    }

    @Override
    public void setID(int recordId) {
        this.recordId = recordId;
    }

    @Override
    public int getID() {
        return recordId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        recordId = ExtUtil.readInt(in);
        instanceId = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        filePath = (String)ExtUtil.read(in, new ExtWrapNullable(String.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.write(out, ExtUtil.emptyIfNull(instanceId));
        ExtUtil.write(out, new ExtWrapNullable(filePath));
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{META_ID};
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (META_ID.equals(fieldName)) {
            return ExtUtil.emptyIfNull(instanceId);
        }
        throw new IllegalArgumentException("No metadata field " + fieldName + " in the form instance storage system");
    }
}

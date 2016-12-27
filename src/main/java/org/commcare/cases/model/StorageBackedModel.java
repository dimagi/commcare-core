package org.commcare.cases.model;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageBackedModel implements Persistable, IMetaData {

    public static final String STORAGE_KEY = "FLATFIX";
    private Hashtable<String, String> attributes = new Hashtable<>();
    private Hashtable<String, String> elements = new Hashtable<>();
    protected int recordId;
    protected String entityId;

    public StorageBackedModel(Hashtable<String, String> attributes,
                              Hashtable<String, String> elements) {
        this.attributes = attributes;
        this.elements = elements;
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    public Hashtable<String, String> getElements() {
        return elements;
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[0];
    }

    @Override
    public Object getMetaData(String fieldName) {
        return null;
    }

    @Override
    public void setID(int id) {
        this.recordId = id;
    }

    @Override
    public int getID() {
        return recordId;
    }

    public String getEntityId() {
        return entityId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        recordId = ExtUtil.readInt(in);
        entityId = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        attributes = (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
        elements = (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(entityId));
        ExtUtil.write(out, new ExtWrapMap(attributes));
        ExtUtil.write(out, new ExtWrapMap(elements));
    }
}

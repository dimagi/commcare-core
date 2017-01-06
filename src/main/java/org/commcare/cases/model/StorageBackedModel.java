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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageBackedModel implements Persistable, IMetaData {

    public static final String STORAGE_KEY_PREFIX = "FLATFIX_";
    private Hashtable<String, String> attributes = new Hashtable<>();
    private Hashtable<String, String> elements = new Hashtable<>();
    private HashSet<String> escapedAttributeKeys = new HashSet<>();
    private HashSet<String> escapedElementKeys = new HashSet<>();
    protected int recordId = -1;
    protected String entityId;
    private String[] metaDataFields = null;

    public StorageBackedModel() {
    }

    /**
     * Input contract: attribute and element hashtable keys must be distinct
     * from each other
     */
    public StorageBackedModel(Hashtable<String, String> attributes,
                              Hashtable<String, String> elements) {
        this.attributes = attributes;
        this.elements = elements;
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    public Set<String> getEscapedAttributeKeys() {
        loadMetaData();
        return escapedAttributeKeys;
    }

    public Hashtable<String, String> getElements() {
        return elements;
    }

    public Set<String> getEscapedElementKeys() {
        loadMetaData();
        return escapedElementKeys;
    }

    @Override
    public String[] getMetaDataFields() {
        loadMetaData();
        return metaDataFields;
    }

    private void loadMetaData() {
        if (metaDataFields == null) {
            metaDataFields = new String[attributes.size() + elements.size()];
            int i = 0;
            for (Enumeration<String> e = attributes.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                String escapedAttr = getColumnName(key);
                escapedAttributeKeys.add(escapedAttr);
                metaDataFields[i++] = escapedAttr;
            }
            for (Enumeration<String> e = elements.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                String escapedElement = getUniqueColumnName(key, escapedAttributeKeys);
                metaDataFields[i++] = escapedElement;
                escapedElementKeys.add(escapedElement);
            }
        }
    }

    @Override
    public Object getMetaData(String fieldName) {
        String unescapedFieldName = removeEscape(fieldName);
        if (escapedAttributeKeys.contains(fieldName)) {
            return attributes.get(unescapedFieldName);
        } else if (escapedElementKeys.contains(fieldName)) {
            return elements.get(unescapedFieldName);
        }

        return null;
    }

    /**
     * escape SQL column name because user may have chosen a fixture element name that collides with a SQL keyword
     */
    public static String getColumnName(String colName) {
        return "_$_" + colName;
    }

    private static String removeEscape(String colName) {
        return colName.substring(colName.indexOf("_", 1));
    }

    public static String getUniqueColumnName(String colName, Set<String> otherColumns) {
        String colNamePre = "_$";
        String uniqColName = "_$_" + colName;
        while (otherColumns.contains(uniqColName)) {
            colNamePre = colNamePre + "$";
            uniqColName = colNamePre + "_" + colName;
        }
        return uniqColName;
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

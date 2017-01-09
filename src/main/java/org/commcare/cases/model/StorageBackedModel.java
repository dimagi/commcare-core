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
 * DB object model, which includes key/value indexes, that gets turned into a
 * flat TreeElement.
 *
 * Flat TreeElements have the following structural constraints:
 * - attributes only appear at the top level
 * - there is only one occurence of every element (they have multiplicity 1)
 * - nested elements are allowed but must follow the aforementioned constraints
 *
 * All attributes and first-level elements will be turned into columns in the
 * associated DB table
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageBackedModel implements Persistable, IMetaData {

    private static final String STORAGE_KEY_PREFIX = "FLATFIX_";

    private Hashtable<String, String> attributes = new Hashtable<>();
    private Hashtable<String, String> elements = new Hashtable<>();
    private Hashtable<String, String> nestedElements = new Hashtable<>();
    private HashSet<String> escapedAttributeKeys = new HashSet<>();
    private HashSet<String> escapedElementKeys = new HashSet<>();

    private String[] metaDataFields = null;

    protected int recordId = -1;
    protected String entityId;

    @SuppressWarnings("unused")
    public StorageBackedModel() {
        // for serialization
    }

    /**
     * Input contract: attribute and element hashtable keys must be distinct
     * from each other and the base of nested elements must not be in the
     * elements set
     */
    public StorageBackedModel(Hashtable<String, String> attributes,
                              Hashtable<String, String> elements,
                              Hashtable<String, String> nestedElements) {
        this.attributes = attributes;
        this.elements = elements;
        this.nestedElements = nestedElements;

        loadMetaData();
    }

    public Hashtable<String, String> getAttributes() {
        return attributes;
    }

    /**
     * @return Attribute names that have been escaped to safely be used as DB
     * column names which are unique from the element column names
     */
    public Set<String> getEscapedAttributeKeys() {
        return escapedAttributeKeys;
    }

    public Hashtable<String, String> getElements() {
        return elements;
    }

    public Hashtable<String, String> getNestedElements() {
        return nestedElements;
    }

    /**
     * @return Element names that have been escaped to safely be used as DB
     * column names which are unique from the attribute column names
     */
    public Set<String> getEscapedElementKeys() {
        return escapedElementKeys;
    }

    @Override
    public String[] getMetaDataFields() {
        return metaDataFields;
    }

    private void loadMetaData() {
        metaDataFields = new String[attributes.size() + elements.size()];
        int i = 0;
        for (Enumeration<String> e = attributes.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            String escapedAttr = getColumnName(key);
            escapedAttributeKeys.add(escapedAttr);
            metaDataFields[i++] = escapedAttr;
        }
        for (Enumeration<String> e = elements.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            String escapedElement = getUniqueColumnName(key, escapedAttributeKeys);
            metaDataFields[i++] = escapedElement;
            escapedElementKeys.add(escapedElement);
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

        throw new IllegalArgumentException("No metadata field " + fieldName +
                " in the storage backed fixture system");
    }

    public static String getTableName(String fixtureName) {
        String cleanedName = fixtureName.replace(":", "_").replace(".", "_").replace("-", "_");
        return STORAGE_KEY_PREFIX + cleanedName;
    }

    /**
     * Escape SQL column name because user may have chosen a fixture element
     * name that collides with a SQL keyword
     */
    public static String getColumnName(String colName) {
        return "_$_" + colName;
    }

    public static String removeEscape(String colName) {
        return colName.substring(colName.indexOf("_", 1) + 1);
    }

    /**
     * Escapes SQL columnn name in a way that guarantees uniqueness from other
     * existing column names
     */
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
        nestedElements = (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf);

        loadMetaData();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(entityId));
        ExtUtil.write(out, new ExtWrapMap(attributes));
        ExtUtil.write(out, new ExtWrapMap(elements));
        ExtUtil.write(out, new ExtWrapMap(nestedElements));
    }
}

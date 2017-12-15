package org.commcare.cases.model;

import org.commcare.cases.instance.FixtureIndexSchema;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * A DB model for storing TreeElements such that particular attributes and
 * elements are indexed and queryable using the DB.
 *
 * Indexed attributes/elements get their own table columns, and the rest of
 * the TreeElement is stored as a serialized blob.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageIndexedTreeElementModel implements Persistable, IMetaData {

    private static final String STORAGE_KEY_PREFIX = "IND_FIX_";
    private static final String DASH_ESCAPE = "\\$\\$";
    private static final int ATTR_PREFIX_LENGTH = "@".length();
    private static final String ATTR_COL_PREFIX = "_$";
    private static final String ELEM_COL_PREFIX = "_0";

    private String[] metaDataFields = null;
    private Vector<String> indices;
    private TreeElement root;

    protected int recordId = -1;
    protected String entityId;

    @SuppressWarnings("unused")
    public StorageIndexedTreeElementModel() {
        // for serialization
    }

    public StorageIndexedTreeElementModel(Set<String> indices, TreeElement root) {
        this.indices = new Vector<>(indices);
        this.root = root;
        metaDataFields = buildMetadataFields(this.indices);
    }

    private static String[] buildMetadataFields(List<String> indices) {
        String[] escapedIndexList = new String[indices.size()];
        int i = 0;
        for (String index : indices) {
            escapedIndexList[i++] = getSqlColumnNameFromElementOrAttribute(index);
        }
        return escapedIndexList;
    }

    public static String getTableName(String fixtureName) {
        String cleanedName = fixtureName.replace(":", "_").replace(".", "_").replace("-", "_");
        return STORAGE_KEY_PREFIX + cleanedName;
    }

    /**
     * @return The list of elements from this model which are indexed, this list will be in the input
     * format, which generally can be interpreted as a treereference step into the model which
     * will reference the metadata field in the virtual instance, IE: "@attributename"
     */
    public Vector<String> getIndexedTreeReferenceSteps() {
        return indices;
    }

    @Override
    public String[] getMetaDataFields() {
        return metaDataFields;
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (fieldName.startsWith(ATTR_COL_PREFIX)) {
            return root.getAttributeValue(null, getElementOrAttributeFromSqlColumnName(fieldName).substring(ATTR_PREFIX_LENGTH));
        } else if (fieldName.startsWith(ELEM_COL_PREFIX)) {
            // NOTE PLM: The usage of getChild of '0' below assumes indexes
            // are only made over entries with multiplicity 0
            TreeElement child = root.getChild(getElementOrAttributeFromSqlColumnName(fieldName), 0);
            if (child == null) {
                return "";
            }
            IAnswerData value = child.getValue();
            if (value == null) {
                return "";
            } else {
                return value.uncast().getString();
            }
        }
        throw new IllegalArgumentException("No metadata field " + fieldName + " in the indexed fixture storage table.");
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

    public TreeElement getRoot() {
        return root;
    }

    public Set<String> getIndexColumnNames() {
        Set<String> indexColumnNames = new HashSet<>();
        for (String index : this.indices) {
            indexColumnNames.add(FixtureIndexSchema.escapeIndex(index));
        }
        return indexColumnNames;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        recordId = ExtUtil.readInt(in);
        entityId = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        root = (TreeElement)ExtUtil.read(in, TreeElement.class, pf);
        indices = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class), pf);
        metaDataFields = buildMetadataFields(indices);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(entityId));
        ExtUtil.write(out, root);
        ExtUtil.write(out, new ExtWrapList(indices));
    }

    public static HashMap<String, String> sqlColumnToElementCache = new HashMap<>();
    public static HashMap<String, String> elementToSqlColumn = new HashMap<>();

    /**
     * Turns a column name into the corresponding attribute or element for the TreeElement
     */
    public static String getElementOrAttributeFromSqlColumnName(String col) {

        if (sqlColumnToElementCache.containsKey(col)) {
            return sqlColumnToElementCache.get(col);
        }
        String input = col;

        col = col.replaceAll(DASH_ESCAPE, "-");
        if (col.startsWith(ATTR_COL_PREFIX)) {
            col = "@" + col.substring(ATTR_COL_PREFIX.length());
        } else if (col.startsWith(ELEM_COL_PREFIX)) {
            col = col.substring(ELEM_COL_PREFIX.length());
        } else {
            throw new RuntimeException("Unable to process index of '" + col + "' metadata entry");
        }

        sqlColumnToElementCache.put(input, col);
        return col;
    }

    /**
     * Turns an attribute or element from the TreeElement into a valid SQL column name
     */
    public static String getSqlColumnNameFromElementOrAttribute(String entry) {
        if (elementToSqlColumn.containsKey(entry)) {
            return elementToSqlColumn.get(entry);
        }

        String input = entry;

        entry = entry.replaceAll("-", DASH_ESCAPE);
        if (entry.startsWith("@")) {
            entry = ATTR_COL_PREFIX + entry.substring(ATTR_PREFIX_LENGTH);
        } else {
            entry = ELEM_COL_PREFIX + entry;
        }
        elementToSqlColumn.put(input, entry);
        return entry;
    }
}

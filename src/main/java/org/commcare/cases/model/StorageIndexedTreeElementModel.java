package org.commcare.cases.model;

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
    private static final String ATTR_PREFIX = "_$";
    private static final String ELEM_PREFIX = "_0";

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
            escapedIndexList[i++] = getColFromEntry(index);
        }
        return escapedIndexList;
    }

    public static String getTableName(String fixtureName) {
        String cleanedName = fixtureName.replace(":", "_").replace(".", "_").replace("-", "_");
        return STORAGE_KEY_PREFIX + cleanedName;
    }

    @Override
    public String[] getMetaDataFields() {
        return metaDataFields;
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (fieldName.startsWith(ATTR_PREFIX)) {
            return root.getAttributeValue(null, getEntryFromCol(fieldName).substring(1));
        } else if (fieldName.startsWith(ELEM_PREFIX)) {
            // NOTE PLM: The usage of getChild of '0' below assumes indexes
            // are only made over entries with multiplicity 0
            TreeElement child = root.getChild(getEntryFromCol(fieldName), 0);
            IAnswerData value = child.getValue();
            if (value == null) {
                return "";
            } else {
                return value.uncast().getString();
            }
        }
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

    public TreeElement getRoot() {
        return root;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
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

    /**
     * Turns a column name into the corresponding attribute or element for the TreeElement
     */
    public static String getEntryFromCol(String col) {
        col = col.replaceAll(DASH_ESCAPE, "-");
        if (col.startsWith(ATTR_PREFIX)) {
            return "@" + col.substring(ATTR_PREFIX.length());
        } else if (col.startsWith(ELEM_PREFIX)) {
            return col.substring(ELEM_PREFIX.length());
        } else {
            throw new RuntimeException("Unable to process index of '" + col + "' metadata entry");
        }
    }

    /**
     * Turns an attribute or element from the TreeElement into a valid SQL column name
     */
    public static String getColFromEntry(String entry) {
        entry = entry.replaceAll("-", DASH_ESCAPE);
        if (entry.startsWith("@")) {
            return ATTR_PREFIX + entry.substring(1);
        } else {
            return ELEM_PREFIX + entry;
        }
    }
}

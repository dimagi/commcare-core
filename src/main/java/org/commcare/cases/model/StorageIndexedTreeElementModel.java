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
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageIndexedTreeElementModel implements Persistable, IMetaData {

    private static final String STORAGE_KEY_PREFIX = "FLATFIX_";
    public static final String ATTR_PREFIX = "_$";
    public static final String ELEM_PREFIX = "_0";

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
            if (index.startsWith("@")) {
                escapedIndexList[i++] = ATTR_PREFIX + index.substring(1);
            } else {
                escapedIndexList[i++] = ELEM_PREFIX + index;
            }
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
            root.getAttributeValue(null, fieldName.substring(2));
        } else if (fieldName.startsWith(ELEM_PREFIX)) {
            TreeElement child = root.getChild(fieldName.substring(2), 0);
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

    public static boolean isAttrCol(String col) {
        return col.startsWith(ATTR_PREFIX);
    }

    public static boolean isElemCol(String col) {
        return col.startsWith(ELEM_PREFIX);
    }

    public static String getAttrFromCol(String col) {
        return "@" + col.substring(ATTR_PREFIX.length());
    }

    public static String getElemFromCol(String col) {
        return col.substring(ELEM_PREFIX.length());
    }

    public boolean areIndicesValid() {
        throw new RuntimeException("Implement me");
    }
}

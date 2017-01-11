package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureSchema {
    private final Set<String> indices;
    public final String fixtureName;

    public FlatFixtureSchema(TreeElement schemaTree) {
        this.fixtureName = schemaTree.getAttributeValue(null, "id");

        indices = buildIndices(schemaTree.getChildrenWithName("index"));
    }

    private static Set<String> buildIndices(Vector<TreeElement> indexElements) {
        HashSet<String> indices = new HashSet<>();
        for (TreeElement index : indexElements) {
            IAnswerData value = index.getValue();
            if (value != null) {
                indices.add(value.uncast().getString());
            }
        }
        return indices;
    }

    public Set<String> getColumnIndices() {
        Set<String> columnIndices = new HashSet<>();
        for (String index : indices) {
            columnIndices.add(escapeIndex(index));
        }
        return columnIndices;
    }

    private static String escapeIndex(String index) {
        if (index.contains(",")) {
            StringBuilder compoundIndex = new StringBuilder();
            String prefix = "";
            for (String entry : index.split(",")) {
                compoundIndex.append(prefix);
                prefix = ",";
                compoundIndex.append(StorageIndexedTreeElementModel.getColFromEntry(entry));
            }
            return compoundIndex.toString();
        } else {
            return StorageIndexedTreeElementModel.getColFromEntry(index);
        }
    }

    public Set<String> getSingleIndices() {
        Set<String> singleIndices = new HashSet<>();
        for (String index : indices) {
            if (index.contains(",")) {
                for (String innerIndex : index.split(",")) {
                    singleIndices.add(innerIndex);
                }
            } else {
                singleIndices.add(index);
            }
        }
        return singleIndices;
    }
}

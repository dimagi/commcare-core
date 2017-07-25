package org.commcare.cases.instance;

import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.util.InvalidStructureException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Tracks what attributes and elements are stored in indexed columns of an
 * indexed fixture db table.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FixtureIndexSchema {
    private final Set<String> indices = new HashSet<>();
    public final String fixtureName;

    public FixtureIndexSchema(TreeElement schemaTree, String fixtureName)
            throws InvalidStructureException {
        this.fixtureName = fixtureName;

        setupIndices(schemaTree.getChildrenWithName("index"));
    }

    private void setupIndices(Vector<TreeElement> indexElements) throws InvalidStructureException {
        for (TreeElement index : indexElements) {
            IAnswerData value = index.getValue();
            if (value != null) {
                String indexString = value.uncast().getString();
                validateIndexValue(indexString);
                indices.add(indexString);
            }
        }
    }

    private static void validateIndexValue(String index) throws InvalidStructureException {
        if (!Pattern.matches("^[a-zA-Z0-9,@_\\.-]+$", index)) {
            throw new InvalidStructureException("Fixture schema contains an invalid index: '" + index + "'");
        }
    }

    /**
     * Break-up composite indices into individual ones and escape index names
     * to be SQL compatible
     */
    public Set<String> getColumnIndices() {
        Set<String> columnIndices = new HashSet<>();
        for (String index : indices) {
            columnIndices.add(escapeIndex(index));
        }
        return columnIndices;
    }

    public static String escapeIndex(String index) {
        if (index.contains(",")) {
            StringBuilder compoundIndex = new StringBuilder();
            String prefix = "";
            for (String entry : index.split(",")) {
                compoundIndex.append(prefix);
                prefix = ",";
                compoundIndex.append(StorageIndexedTreeElementModel.getSqlColumnNameFromElementOrAttribute(entry));
            }
            return compoundIndex.toString();
        } else {
            return StorageIndexedTreeElementModel.getSqlColumnNameFromElementOrAttribute(index);
        }
    }

    /**
     * Set of indices, breaking apart composite indices
     * i.e. ("id", "name,dob") -> ("id", "name", "dob")
     */
    public Set<String> getSingleIndices() {
        Set<String> singleIndices = new HashSet<>();
        for (String index : indices) {
            if (index.contains(",")) {
                Collections.addAll(singleIndices, index.split(","));
            } else {
                singleIndices.add(index);
            }
        }
        return singleIndices;
    }
}

package org.commcare.cases.instance;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;

import java.util.ArrayList;
import java.util.HashMap;
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

    public Set<String> getIndices() {
        return indices;
    }

    public Set<String> getSingleIndices() {
        Set<String> singleIndices = new HashSet<>();
        for (String index : indices) {
            if (!index.contains(",")) {
                singleIndices.add(index);
            }
        }
        return singleIndices;
    }


}

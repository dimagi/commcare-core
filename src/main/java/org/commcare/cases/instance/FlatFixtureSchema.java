package org.commcare.cases.instance;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FlatFixtureSchema {
    private final Set<String> elementSet;
    private final Set<String> attributeSet;
    private final Map<String, List<String>> nestedElementSet = new HashMap<>();
    private final Set<String> indices;
    public final String baseName, childName, fixtureName;

    public FlatFixtureSchema(TreeElement schemaTree) {
        TreeElement structureBase = schemaTree.getChild("structure", 0).getChildAt(0);
        TreeElement entrySchema = structureBase.getChildAt(0);
        this.baseName = structureBase.getName();
        this.childName = entrySchema.getName();
        this.fixtureName = schemaTree.getAttributeValue(null, "id");

        indices = buildIndices(schemaTree.getChild("indices", 0));
        elementSet = buildExpectedElements(entrySchema, nestedElementSet);
        attributeSet = buildAttributeKeys(entrySchema);
    }

    private static Set<String> buildIndices(TreeElement indiceRoot) {
        HashSet<String> indices = new HashSet<>();
        for (int i = 0; i < indiceRoot.getNumChildren(); i++) {
            TreeElement entry = indiceRoot.getChildAt(i);
            IAnswerData value = entry.getValue();
            if (value != null) {
                indices.add(value.uncast().getString());
            }
        }
        return indices;
    }

    private static Set<String> buildAttributeKeys(TreeElement root) {
        HashSet<String> attributeSet = new HashSet<>();
        for (int i = 0; i < root.getAttributeCount(); i++) {
            attributeSet.add(root.getAttributeName(i));
        }

        return attributeSet;
    }

    private static Set<String> buildExpectedElements(TreeElement root,
                                                     Map<String, List<String>> nestedElementSet) {
        HashSet<String> elementSet = new HashSet<>();
        for (int i = 0; i < root.getNumChildren(); i++) {
            TreeElement entry = root.getChildAt(i);

            if (entry.getMult() != 0) {
                throw new RuntimeException("'" + entry.getName() + "' appears more than once in the flat fixture schema");
            }

            if (entry.hasChildren()) {
                buildNestedExpectedElements(nestedElementSet, entry);
            } else {
                elementSet.add(entry.getName());
            }
        }

        return elementSet;
    }

    private static void buildNestedExpectedElements(Map<String, List<String>> nestedElementSet, TreeElement entry) {
        for (int i = 0; i < entry.getNumChildren(); i++) {
            TreeElement nestedElement = entry.getChildAt(i);

            if (nestedElement.getNumChildren() != 0 || nestedElement.getAttributeCount() != 0) {
                throw new RuntimeException("Nested elements in flat fixture cannot have children or attributes");
            }

            if (nestedElement.getMult() != 0) {
                throw new RuntimeException("Flat fixture doesn't have a table structure: has more than one entry with the same name");
            }

            String entryName = entry.getName();
            if (!nestedElementSet.containsKey(entryName)) {
                nestedElementSet.put(entryName, new ArrayList<String>());
            }
            nestedElementSet.get(entryName).add(nestedElement.getName());
        }
    }

    public boolean isAttributeInSchema(String attribute) {
        return attributeSet.contains(attribute);
    }

    public boolean assertElementInSchema(TreeElement elem) {
        if (elem.getMult() != 0) {
            return false;
        }

        String elemName = elem.getName();
        if (elementSet.contains(elemName)) {
            return true;
        }

        if (!elem.hasChildren() && nestedElementSet.containsKey(elemName)) {
            return false;
        }

        if (elem.getParent() != null) {
            String parentName = elem.getParent().getName();
            if (nestedElementSet.containsKey(parentName)
                    && nestedElementSet.get(parentName).contains(elemName)) {
                return true;
            }
        }

        throw new RuntimeException();
    }

    public Set<String> getIndices() {
        return indices;
    }


}

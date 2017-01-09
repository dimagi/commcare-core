package org.commcare.xml;

import org.commcare.cases.model.StorageBackedModel;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * Creates a table for the flat fixture and parses each element into a
 * StorageBackedModel and stores that as a table row.
 *
 * Also stores base and child names associated with fixture in another database.
 * For example, if we have a fixture referenced by
 * instance('product-list')/products/product/... then we need to associate
 * ('product-list', 'products', 'product') to be able to reconstruct the
 * fixture instance
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class FlatFixtureXmlParser extends TransactionParser<StorageBackedModel> {

    private static final HashSet<String> flatSet = new HashSet<>();

    static {
        FlatFixtureXmlParser.flatSet.add("locations");
    }

    public FlatFixtureXmlParser(KXmlParser parser) {
        super(parser);
    }

    public static boolean isFlatDebug(String id) {
        if (id.startsWith("jr://fixture/")) {
            id = id.substring(13);
        }
        return flatSet.contains(id);
    }

    @Override
    public StorageBackedModel parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
        checkNode("fixture");

        String fixtureId = parser.getAttributeValue(null, "id");
        if (fixtureId == null) {
            throw new InvalidStructureException("fixture is lacking id attribute", parser);
        }

        if (nextTagInBlock("fixture")) {
            // only commit fixtures with bodies to storage
            TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
            processRoot(root, fixtureId);
        }

        return null;
    }

    private void processRoot(TreeElement root, String fixtureId) throws IOException {
        if (root.hasChildren()) {
            TreeElement firstChild = root.getChildAt(0);
            HashSet<String> expectedElements = buildExpectedElements(firstChild);
            HashSet<String> expectedAttributes = buildAttributeKeys(firstChild);

            writeFixtureIndex(fixtureId, root.getName(), firstChild.getName());

            for (TreeElement entry : root.getChildrenWithName(firstChild.getName())) {
                processEntry(entry, expectedElements, expectedAttributes);
            }
        }
    }

    private static HashSet<String> buildAttributeKeys(TreeElement root) {
        HashSet<String> attributeSet = new HashSet<>();
        for (int i = 0; i < root.getAttributeCount(); i++) {
            attributeSet.add(root.getAttributeName(i));
        }

        return attributeSet;
    }

    private static HashSet<String> buildExpectedElements(TreeElement root) {
        HashSet<String> elementSet = new HashSet<>();
        for (int i = 0; i < root.getNumChildren(); i++) {
            TreeElement entry = root.getChildAt(i);

            if (elementSet.contains(entry.getName())) {
                throw new RuntimeException("Flat fixture doesn't have a table structure: has more than one entry with the same name");
            }

            if (entry.hasChildren()) {
                buildNestedExpectedElements(elementSet, entry);
            } else {
                elementSet.add(entry.getName());
            }
        }

        return elementSet;
    }

    private static void buildNestedExpectedElements(Set<String> elementSet, TreeElement entry) {
        for (int i = 0; i < entry.getNumChildren(); i++) {
            TreeElement nestedElement = entry.getChildAt(i);

            if (nestedElement.getNumChildren() != 0 || nestedElement.getAttributeCount() != 0) {
                throw new RuntimeException("Nested elements in flat fixture cannot have children or attributes");
            }

            String nestedName = getNestedName(entry, nestedElement);

            if (elementSet.contains(nestedName)) {
                throw new RuntimeException("Flat fixture doesn't have a table structure: has more than one entry with the same name");
            }

            elementSet.add(nestedName);
        }
    }

    private static String getNestedName(TreeElement parent, TreeElement child) {
        return parent.getName() + "/" + child.getName();
    }

    private void processEntry(TreeElement child,
                              HashSet<String> expectedElements,
                              HashSet<String> expectedAttributes) throws IOException {
        HashSet<String> expectedElementsCopy = new HashSet<>(expectedElements);
        Hashtable<String, String> elements = new Hashtable<>();
        Hashtable<String, String> nestedElements = new Hashtable<>();
        loadElements(child, elements, nestedElements, expectedElementsCopy);

        HashSet<String> expectedAttributesCopy = new HashSet<>(expectedAttributes);
        Hashtable<String, String> attributes = loadAttributes(child, expectedAttributesCopy);

        StorageBackedModel model = new StorageBackedModel(attributes, elements, nestedElements);
        commit(model);
    }

    private static void loadElements(TreeElement child,
                                     Hashtable<String, String> elements,
                                     Hashtable<String, String> nestedElements,
                                     Set<String> expectedElements) {
        for (int i = 0; i < child.getNumChildren(); i++) {
            TreeElement entry = child.getChildAt(i);
            if (entry.hasChildren()) {
                loadNestedElements(entry, nestedElements, expectedElements);
            } else {
                if (assertInExpectedOrEmptyNested(expectedElements, entry.getName())) {
                    IAnswerData value = entry.getValue();
                    elements.put(entry.getName(), value == null ? "" : value.uncast().getString());
                }
            }
        }
    }

    private static boolean assertInExpectedOrEmptyNested(Set<String> expectedElements,
                                                         String name) {
        if (!expectedElements.remove(name)) {
            // we allow elements with children to be empty, so check if it is
            // expected that this element has children
            for (String expectedElem : expectedElements) {
                if (expectedElem.startsWith(name + "/")) {
                    return false;
                }
            }
            throw new RuntimeException("Flat fixture isn't homogeneous");
        }
        return true;
    }

    private static void loadNestedElements(TreeElement entry,
                                           Hashtable<String, String> nestedElements,
                                           Set<String> expectedElements) {
        for (int i = 0; i < entry.getNumChildren(); i++) {
            TreeElement child = entry.getChildAt(i);
            String nestedName = getNestedName(entry, child);

            if (!expectedElements.remove(nestedName)) {
                throw new RuntimeException("Flat fixture isn't homogeneous");
            }
            IAnswerData value = child.getValue();
            nestedElements.put(nestedName, value == null ? "" : value.uncast().getString());
        }
    }

    private static Hashtable<String, String> loadAttributes(TreeElement child,
                                                            HashSet<String> expectedAttributes) {
        Hashtable<String, String> attributes = new Hashtable<>();
        for (int i = 0; i < child.getAttributeCount(); i++) {
            String attrName = child.getAttributeName(i);
            TreeElement attr = child.getAttribute(null, attrName);
            if (!expectedAttributes.remove(attr.getName())) {
                throw new RuntimeException("Flat fixture isn't homogeneous");
            }
            attributes.put(attr.getName(), attr.getValue().uncast().getString());
        }
        return attributes;
    }

    @Override
    protected void commit(StorageBackedModel parsed) throws IOException {
        try {
            getFlatFixtureStorage(parsed).write(parsed);
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    /**
     * Get storage that stores fixture element entries as table rows
     */
    public abstract IStorageUtilityIndexed<StorageBackedModel> getFlatFixtureStorage(StorageBackedModel exampleEntry);

    /**
     * Store base and child node names associated with a fixture.
     * Used for reconstructiong fixture instance
     */
    public abstract void writeFixtureIndex(String fixtureName, String baseName, String childName);
}

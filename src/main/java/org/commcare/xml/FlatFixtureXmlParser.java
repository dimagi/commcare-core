package org.commcare.xml;

import org.commcare.cases.model.StorageBackedModel;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * The Fixture XML Parser is responsible for parsing incoming fixture data and
 * storing it as a file with a pointer in a db.
 *
 * @author ctsims
 */
public class FlatFixtureXmlParser extends TransactionParser<StorageBackedModel> {

    IStorageUtilityIndexed<StorageBackedModel> storage;
    private final boolean overwrite;

    public FlatFixtureXmlParser(KXmlParser parser) {
        this(parser, true, null);
    }

    public FlatFixtureXmlParser(KXmlParser parser, boolean overwrite,
                                IStorageUtilityIndexed<StorageBackedModel> storage) {
        super(parser);
        this.overwrite = overwrite;
        this.storage = storage;
    }

    @Override
    public StorageBackedModel parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
        this.checkNode("fixture");

        String fixtureId = parser.getAttributeValue(null, "id");
        if (fixtureId == null) {
            throw new InvalidStructureException("fixture is lacking id attribute", parser);
        }

        String userId = parser.getAttributeValue(null, "user_id");

        TreeElement root;
        if (!nextTagInBlock("fixture")) {
            // fixture with no body; don't commit to storage
            return null;
        }

        root = new TreeElementParser(parser, 0, fixtureId).parse();

        if (root.hasChildren()) {
            TreeElement firstChild = root.getChildAt(0);
            int childCount = firstChild.getNumChildren();
            HashSet<String> expectedElements = buildChildKeys(firstChild);
            if (expectedElements.size() != childCount) {
                throw new RuntimeException("Flat fixture doesn't have a table structure: has more than one entry with the same name");
            }
            HashSet<String> expectedAttributes = buildAttributeKeys(firstChild);

            for (TreeElement child : root.getChildrenWithName(firstChild.getName())) {
                processChild(child, expectedElements, expectedAttributes);
            }
        }
    }

    private static HashSet<String> buildAttributeKeys(TreeElement root) {
        HashSet<String> attributeSet = new HashSet<>();
        for (int i = 0; i <= root.getAttributeCount(); i++) {
            attributeSet.add(root.getAttributeName(i));
        }

        return attributeSet;
    }

    private static HashSet<String> buildChildKeys(TreeElement root) {
        HashSet<String> elementSet = new HashSet<>();
        for (int i = 0; i <= root.getNumChildren(); i++) {
            elementSet.add(root.getChildAt(i).getName());
        }

        return elementSet;
    }

    private void processChild(TreeElement child,
                              HashSet<String> expectedElements,
                              HashSet<String> expectedAttributes) throws IOException {
        HashSet<String> expectedElementsCopy = new HashSet<>(expectedElements);
        Hashtable<String, String> elements = loadElements(child, expectedElementsCopy);

        HashSet<String> expectedAttributesCopy = new HashSet<>(expectedAttributes);
        Hashtable<String, String> attributes = loadAttributes(child, expectedAttributesCopy);

        StorageBackedModel model = new StorageBackedModel(attributes, elements);
        commit(model);
    }

    private Hashtable<String, String> loadElements(TreeElement child,
                                                   HashSet<String> expectedElementsCopy) {
        Hashtable<String, String> elements = new Hashtable<>();
        for (int i = 0; i <= child.getNumChildren(); i++) {
            TreeElement entry = child.getChildAt(i);
            if (!expectedElementsCopy.remove(entry.getName())) {
                throw new RuntimeException("Flat fixture is heterogeneous");
            }
            elements.put(entry.getName(), entry.getValue().uncast().getString());
        }
        return elements;
    }

    private Hashtable<String, String> loadAttributes(TreeElement child,
                                                     HashSet<String> expectedAttributesCopy) {
        Hashtable<String, String> attributes = new Hashtable<>();
        for (int i = 0; i <= child.getAttributeCount(); i++) {
            String attrName = child.getAttributeName(i);
            TreeElement attr = child.getAttribute(null, attrName);
            if (!expectedAttributesCopy.remove(attr.getName())) {
                throw new RuntimeException("Flat fixture is heterogeneous");
            }
            attributes.put(attr.getName(), attr.getValue().uncast().getString());
        }
        return attributes;
    }

    @Override
    protected void commit(StorageBackedModel parsed) throws IOException {
        try {
            storage().write(parsed);
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    public IStorageUtilityIndexed<StorageBackedModel> storage() {
        if (storage == null) {
            storage = (IStorageUtilityIndexed)StorageManager.getStorage(StorageBackedModel.STORAGE_KEY);
        }
        return storage;
    }
}

package org.commcare.xml;

import org.commcare.cases.model.StorageBackedModel;
import org.commcare.data.xml.TransactionParser;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

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
        //TODO: We need to overwrite any matching records here.
        root = new TreeElementParser(parser, 0, fixtureId).parse();

        if (root.hasChildren()) {
            TreeElement firstChild = root.getChildAt(0);
            int expectedSize = firstChild.getNumChildren();
            HashSet<String> expectedNames = buildChildKeys(firstChild);
            if (expectedNames.size() != expectedSize) {
                throw new RuntimeException("Flat fixture doesn't have a table structure: has more than one entry with the same name");
            }

            for (TreeElement child : root.getChildrenWithName(firstChild.getName())) {
                processChild(child, expectedSize, expectedNames);
            }
        }
    }

    private HashSet<String> buildChildKeys(TreeElement root) {
        HashSet<String> childNameSet = new HashSet<>();
        for (int i = 0; i <= root.getNumChildren(); i++) {
            childNameSet.add(root.getChildAt(i).getName());
        }

        return childNameSet;
    }

    private void processChild(TreeElement child, int expectedSize,
                              HashSet<String> expectedNames) throws IOException {
        HashSet<String> expectedNamesCopy = new HashSet<>(expectedNames);
        if (expectedSize != child.getNumChildren()) {
            throw new RuntimeException("Flat fixture is heterogeneous");
        }

        Hashtable<String, String> attributes = new Hashtable<>();
        Hashtable<String, String> elements = new Hashtable<>();
        for (int i = 0; i <= child.getNumChildren(); i++) {
            TreeElement entry = child.getChildAt(i);
            if (!expectedNamesCopy.remove(entry.getName())) {
                throw new RuntimeException("Flat fixture is heterogeneous");
            }
            elements.put(entry.getName(), entry.getValue().uncast().getString());
        }
        StorageBackedModel model = new StorageBackedModel(attributes, elements);
        commit(model);
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

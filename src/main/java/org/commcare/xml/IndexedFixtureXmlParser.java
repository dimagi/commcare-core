package org.commcare.xml;

import org.commcare.cases.instance.FixtureIndexSchema;
import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.data.xml.TransactionParser;
import org.commcare.modern.util.Pair;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates a table for the indexed fixture and parses each element into a
 * StorageIndexedTreeElementModel and stores that as a table row.
 *
 * Also stores base and child names associated with fixture in another database.
 * For example, if we have a fixture referenced by
 * instance('product-list')/products/product/... then we need to associate
 * ('product-list', 'products', 'product') to be able to reconstruct the
 * fixture instance
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixtureXmlParser extends TransactionParser<StorageIndexedTreeElementModel> {

    private final Set<String> indices;
    private final Set<String> columnIndices;
    private final UserSandbox sandbox;
    private final String fixtureName;
    private IStorageUtilityIndexed<StorageIndexedTreeElementModel> indexedFixtureStorage;
    private IStorageUtilityIndexed<FormInstance> normalFixtureStorage;

    public IndexedFixtureXmlParser(KXmlParser parser, String fixtureId,
                                   FixtureIndexSchema schema, UserSandbox sandbox) {
        super(parser);
        this.sandbox = sandbox;
        this.fixtureName = fixtureId;

        if (schema == null) {
            // don't create any table indices if there was no fixture index schema
            this.indices = new HashSet<>();
            this.columnIndices = new HashSet<>();
        } else {
            this.indices = schema.getSingleIndices();
            this.columnIndices = schema.getColumnIndices();
        }
    }

    @Override
    public StorageIndexedTreeElementModel parse() throws InvalidStructureException, IOException,
            XmlPullParserException, UnfullfilledRequirementsException {
        checkNode("fixture");

        if (fixtureName == null) {
            throw new InvalidStructureException("fixture is lacking id attribute", parser);
        }

        if (nextTagInBlock("fixture")) {
            // only commit fixtures with bodies to storage
            TreeElement root = new TreeElementParser(parser, 0, fixtureName).parse();
            processRoot(root);

            // commit whole instance to normal fixture storage to allow for
            // migrations going forward, if ever needed
            String userId = parser.getAttributeValue(null, "user_id");
            Pair<FormInstance, Boolean> instanceAndCommitStatus =
                    FixtureXmlParser.setupInstance(getNormalFixtureStorage(),
                            root, fixtureName, userId, true);
            commitToNormalStorage(instanceAndCommitStatus.first);
        }

        return null;
    }

    private void processRoot(TreeElement root) throws IOException {
        if (root.hasChildren()) {
            String entryName = root.getChildAt(0).getName();
            writeFixtureIndex(root, entryName);

            for (TreeElement entry : root.getChildrenWithName(entryName)) {
                processEntry(entry, indices);
            }
        } else {
            IStorageUtilityIndexed storage = sandbox.getIndexedFixtureStorage(fixtureName);
            if (storage != null) {
                storage.removeAll();
            }
        }
    }

    private void processEntry(TreeElement child, Set<String> indices) throws IOException {
        StorageIndexedTreeElementModel model = new StorageIndexedTreeElementModel(indices, child);
        commit(model);
    }

    @Override
    protected void commit(StorageIndexedTreeElementModel parsed) throws IOException {
        getIndexedFixtureStorage(parsed).write(parsed);
    }

    private void commitToNormalStorage(FormInstance instance) throws IOException {
        getNormalFixtureStorage().write(instance);
    }

    /**
     * Get storage that stores fixture element entries as table rows
     */
    private IStorageUtilityIndexed<StorageIndexedTreeElementModel> getIndexedFixtureStorage(StorageIndexedTreeElementModel exampleEntry) {
        if (indexedFixtureStorage == null) {
            sandbox.setupIndexedFixtureStorage(fixtureName, exampleEntry, columnIndices);
            indexedFixtureStorage = sandbox.getIndexedFixtureStorage(fixtureName);
        }
        return indexedFixtureStorage;
    }

    private IStorageUtilityIndexed<FormInstance> getNormalFixtureStorage() {
        if (normalFixtureStorage == null) {
            normalFixtureStorage = sandbox.getUserFixtureStorage();
        }
        return normalFixtureStorage;
    }

    /**
     * Store base and child node names associated with a fixture.
     * Used for reconstructing fixture instance
     */
    private void writeFixtureIndex(TreeElement root, String childName) {

        TreeElement attrholder = new TreeElement(root.getName());
        for (int i = 0; i < root.getAttributeCount(); i++) {
            attrholder.setAttribute(root.getAttributeNamespace(i), root.getAttributeName(i), root.getAttributeValue(i));
        }

        sandbox.setIndexedFixturePathBases(fixtureName, root.getName(), childName, attrholder);
    }
}

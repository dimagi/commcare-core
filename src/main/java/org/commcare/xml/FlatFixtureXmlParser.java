package org.commcare.xml;

import org.commcare.cases.instance.FixtureIndexSchema;
import org.commcare.cases.model.StorageIndexedTreeElementModel;
import org.commcare.core.interfaces.UserSandbox;
import org.commcare.data.xml.TransactionParser;
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
import java.util.Set;

/**
 * Creates a table for the flat fixture and parses each element into a
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
public class FlatFixtureXmlParser extends TransactionParser<StorageIndexedTreeElementModel> {

    private final Set<String> indices;
    private final Set<String> columnIndices;
    private static final HashSet<String> flatSet = new HashSet<>();
    private final UserSandbox sandbox;
    private final String fixtureName;
    private IStorageUtilityIndexed<StorageIndexedTreeElementModel> flatFixtureStorage;

    static {
        FlatFixtureXmlParser.flatSet.add("locations");
    }

    public FlatFixtureXmlParser(KXmlParser parser, String fixtureName,
                                FixtureIndexSchema schema, UserSandbox sandbox) {
        super(parser);
        this.sandbox = sandbox;
        this.fixtureName = fixtureName;

        if (schema == null) {
            // don't create any table indices if there was no fixture index schema
            this.indices = new HashSet<>();
            this.columnIndices = new HashSet<>();
        } else {
            this.indices = schema.getSingleIndices();
            this.columnIndices = schema.getColumnIndices();
        }
    }

    public static boolean isFlatDebug(String id) {
        if (id.startsWith("jr://fixture/")) {
            id = id.substring(13);
        }
        return flatSet.contains(id);
    }

    @Override
    public StorageIndexedTreeElementModel parse() throws InvalidStructureException, IOException,
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
            String entryName = root.getChildAt(0).getName();
            writeFixtureIndex(fixtureId, root.getName(), entryName);

            for (TreeElement entry : root.getChildrenWithName(entryName)) {
                processEntry(entry, indices);
            }
        }
    }

    private void processEntry(TreeElement child, Set<String> indices) throws IOException {
        StorageIndexedTreeElementModel model = new StorageIndexedTreeElementModel(indices, child);

        commit(model);
    }

    @Override
    protected void commit(StorageIndexedTreeElementModel parsed) throws IOException {
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
    private IStorageUtilityIndexed<StorageIndexedTreeElementModel> getFlatFixtureStorage(StorageIndexedTreeElementModel exampleEntry) {
        if (flatFixtureStorage == null) {
            sandbox.setupFlatFixtureStorage(fixtureName, exampleEntry, columnIndices);
            flatFixtureStorage = sandbox.getFlatFixtureStorage(fixtureName);
        }
        return flatFixtureStorage;
    }

    /**
     * Store base and child node names associated with a fixture.
     * Used for reconstructiong fixture instance
     */
    private void writeFixtureIndex(String fixtureName, String baseName, String childName) {
        sandbox.setFlatFixturePathBases(fixtureName, baseName, childName);
    }
}

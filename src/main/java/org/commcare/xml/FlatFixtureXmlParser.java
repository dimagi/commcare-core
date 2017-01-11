package org.commcare.xml;

import org.commcare.cases.instance.FlatFixtureSchema;
import org.commcare.cases.model.StorageIndexedTreeElementModel;
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
public abstract class FlatFixtureXmlParser extends TransactionParser<StorageIndexedTreeElementModel> {

    private final FlatFixtureSchema schema;
    private static final HashSet<String> flatSet = new HashSet<>();

    static {
        FlatFixtureXmlParser.flatSet.add("locations");
    }

    public FlatFixtureXmlParser(KXmlParser parser, FlatFixtureSchema schema) {
        super(parser);

        this.schema = schema;
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
        Set<String> singleIndices = schema.getSingleIndices();
        if (root.hasChildren()) {
            writeFixtureIndex(fixtureId, schema.baseName, schema.childName);

            for (TreeElement entry : root.getChildrenWithName(schema.childName)) {
                processEntry(entry, singleIndices);
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
    public abstract IStorageUtilityIndexed<StorageIndexedTreeElementModel> getFlatFixtureStorage(StorageIndexedTreeElementModel exampleEntry);

    /**
     * Store base and child node names associated with a fixture.
     * Used for reconstructiong fixture instance
     */
    public abstract void writeFixtureIndex(String fixtureName, String baseName, String childName);
}

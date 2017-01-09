package org.commcare.xml;

import org.commcare.cases.instance.FlatFixtureSchema;
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
            writeFixtureIndex(fixtureId, schema.baseName, schema.childName);

            for (TreeElement entry : root.getChildrenWithName(schema.childName)) {
                processEntry(entry);
            }
        }
    }


    private static String getNestedName(TreeElement parent, TreeElement child) {
        return parent.getName() + "/" + child.getName();
    }

    private void processEntry(TreeElement child) throws IOException {
        Hashtable<String, String> elements = new Hashtable<>();
        Hashtable<String, String> nestedElements = new Hashtable<>();
        loadElements(child, elements, nestedElements);

        Hashtable<String, String> attributes = loadAttributes(child);

        StorageBackedModel model = new StorageBackedModel(attributes, elements, nestedElements);
        commit(model);
    }

    private void loadElements(TreeElement child,
                              Hashtable<String, String> elements,
                              Hashtable<String, String> nestedElements) {
        for (int i = 0; i < child.getNumChildren(); i++) {
            TreeElement entry = child.getChildAt(i);
            if (entry.hasChildren()) {
                loadNestedElements(entry, nestedElements);
            } else {
                if (schema.assertElementInSchema(entry)) {
                    IAnswerData value = entry.getValue();
                    elements.put(entry.getName(), value == null ? "" : value.uncast().getString());
                }
            }
        }
    }

    private void loadNestedElements(TreeElement entry,
                                    Hashtable<String, String> nestedElements) {
        for (int i = 0; i < entry.getNumChildren(); i++) {
            TreeElement child = entry.getChildAt(i);
            String nestedName = getNestedName(entry, child);

            schema.assertElementInSchema(child);
            IAnswerData value = child.getValue();
            nestedElements.put(nestedName, value == null ? "" : value.uncast().getString());
        }
    }

    private Hashtable<String, String> loadAttributes(TreeElement child) {
        Hashtable<String, String> attributes = new Hashtable<>();
        for (int i = 0; i < child.getAttributeCount(); i++) {
            String attrName = child.getAttributeName(i);
            TreeElement attr = child.getAttribute(null, attrName);

            if (!schema.isAttributeInSchema(attr.getName())) {
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

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

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public abstract class FlatFixtureXmlParser extends TransactionParser<StorageBackedModel> {

    private static final HashSet<String> flatSet = new HashSet<>();
    static {
        FlatFixtureXmlParser.flatSet.add("locations");
        FlatFixtureXmlParser.flatSet.add("item-list:wfl_0_2_zscores");
        FlatFixtureXmlParser.flatSet.add("item-list:wfa_0_5_zscores");
        FlatFixtureXmlParser.flatSet.add("item-list:wfa_0_13_zscores");
        FlatFixtureXmlParser.flatSet.add("item-list:lhfa_0_13_zscores");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_meds_g1");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_acts");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_equipments1");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_meds_g4");
        FlatFixtureXmlParser.flatSet.add("item-list:wfh_2_5_zscores");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_meds_g5");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_meds_g3");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_group_eq");
        FlatFixtureXmlParser.flatSet.add("item-list:lhfa_0_5_zscores");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_meds_g2");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_group_meds");
        FlatFixtureXmlParser.flatSet.add("item-list:amu_equipments2");
    }

    IStorageUtilityIndexed<StorageBackedModel> storage;

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
        this.checkNode("fixture");

        String fixtureId = parser.getAttributeValue(null, "id");
        if (fixtureId == null) {
            throw new InvalidStructureException("fixture is lacking id attribute", parser);
        }

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

            writeFixtureIndex(fixtureId, root.getName(), firstChild.getName());

            for (TreeElement child : root.getChildrenWithName(firstChild.getName())) {
                processChild(child, expectedElements, expectedAttributes);
            }
        }
        return null;
    }

    private static HashSet<String> buildAttributeKeys(TreeElement root) {
        HashSet<String> attributeSet = new HashSet<>();
        for (int i = 0; i < root.getAttributeCount(); i++) {
            attributeSet.add(root.getAttributeName(i));
        }

        return attributeSet;
    }

    private static HashSet<String> buildChildKeys(TreeElement root) {
        HashSet<String> elementSet = new HashSet<>();
        for (int i = 0; i < root.getNumChildren(); i++) {
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

    private static Hashtable<String, String> loadElements(TreeElement child,
                                                          HashSet<String> expectedElementsCopy) {
        Hashtable<String, String> elements = new Hashtable<>();
        for (int i = 0; i < child.getNumChildren(); i++) {
            TreeElement entry = child.getChildAt(i);
            if (!expectedElementsCopy.remove(entry.getName())) {
                throw new RuntimeException("Flat fixture is heterogeneous");
            }
            IAnswerData value = entry.getValue();
            elements.put(entry.getName(), value == null ? "" : value.uncast().getString());
        }
        return elements;
    }

    private static Hashtable<String, String> loadAttributes(TreeElement child,
                                                            HashSet<String> expectedAttributesCopy) {
        Hashtable<String, String> attributes = new Hashtable<>();
        for (int i = 0; i < child.getAttributeCount(); i++) {
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
            fixtureStorage(parsed).write(parsed);
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    public abstract IStorageUtilityIndexed<StorageBackedModel> fixtureStorage(StorageBackedModel exampleEntry);

    public abstract void writeFixtureIndex(String fixtureName, String baseName, String childName);
}

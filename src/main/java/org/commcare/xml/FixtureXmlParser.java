package org.commcare.xml;

import org.commcare.data.xml.TransactionParser;
import org.commcare.modern.util.Pair;
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
import java.util.Vector;

/**
 * The Fixture XML Parser is responsible for parsing incoming fixture data and
 * storing it as a file with a pointer in a db.
 *
 * @author ctsims
 */
public class FixtureXmlParser extends TransactionParser<FormInstance> {

    IStorageUtilityIndexed<FormInstance> storage;
    private final boolean overwrite;

    public FixtureXmlParser(KXmlParser parser) {
        this(parser, true, null);
    }

    public FixtureXmlParser(KXmlParser parser, boolean overwrite,
                            IStorageUtilityIndexed<FormInstance> storage) {
        super(parser);
        this.overwrite = overwrite;
        this.storage = storage;
    }

    @Override
    public FormInstance parse() throws InvalidStructureException, IOException,
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

        Pair<FormInstance, Boolean> instanceAndCommitStatus =
                setupInstance(storage(), root, fixtureId, userId, overwrite);

        if (instanceAndCommitStatus.second) {
            commit(instanceAndCommitStatus.first);
        }

        return instanceAndCommitStatus.first;
    }

    private static Pair<FormInstance, Boolean> setupInstance(IStorageUtilityIndexed<FormInstance> storage,
                                                             TreeElement root, String fixtureId,
                                                             String userId, boolean overwrite) {
        FormInstance instance = new FormInstance(root, fixtureId);

        //This is a terrible hack and clayton should feeel terrible about it
        if (userId != null) {
            instance.schema = userId;
        }

        //If we're using storage, deal properly
        if (storage != null) {
            int recordId = -1;
            Vector<Integer> matchingFixtures = storage.getIDsForValue(FormInstance.META_ID, fixtureId);
            if (matchingFixtures.size() > 0) {
                //find all fixtures with the same user
                Vector<Integer> matchingUsers = storage.getIDsForValue(FormInstance.META_XMLNS, ExtUtil.emptyIfNull(userId));
                for (Integer i : matchingFixtures) {
                    if (matchingUsers.indexOf(i) != -1) {
                        recordId = i;
                    }
                }
            }

            if (recordId != -1) {
                if (!overwrite) {
                    //parse it out, but don't write anything to memory if one already exists
                    return Pair.create(instance, false);
                }
                instance.setID(recordId);
            }
        }
        return Pair.create(instance, true);
    }

    @Override
    protected void commit(FormInstance parsed) throws IOException {
        try {
            storage().write(parsed);
        } catch (StorageFullException e) {
            e.printStackTrace();
            throw new IOException("Storage full while writing case!");
        }
    }

    public IStorageUtilityIndexed<FormInstance> storage() {
        if (storage == null) {
            storage = (IStorageUtilityIndexed)StorageManager.getStorage(FormInstance.STORAGE_KEY);
        }
        return storage;
    }
}

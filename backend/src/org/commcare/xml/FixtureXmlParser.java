/**
 *
 */
package org.commcare.xml;

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
import java.util.Vector;

/**
 * The CaseXML Parser is responsible for processing and performing
 * case transactions from an incoming XML stream. It will perform
 * all of the actions specified by the transaction (Create/modify/close)
 * against the application's current storage.
 *
 * @author ctsims
 */
public class FixtureXmlParser extends TransactionParser<FormInstance> {

    IStorageUtilityIndexed<FormInstance> storage;
    boolean overwrite = true;

    public FixtureXmlParser(KXmlParser parser) {
        this(parser, true, null);
    }

    public FixtureXmlParser(KXmlParser parser, boolean overwrite, IStorageUtilityIndexed<FormInstance> storage) {
        super(parser);
        this.overwrite = overwrite;
        this.storage = storage;
    }

    public FormInstance parse() throws InvalidStructureException, IOException, XmlPullParserException, UnfullfilledRequirementsException, StorageFullException {
        this.checkNode("fixture");

        String fixtureId = parser.getAttributeValue(null, "id");
        if (fixtureId == null) {
            throw new InvalidStructureException("fixture is lacking id attribute", parser);
        }

        String userId = parser.getAttributeValue(null, "user_id");

        //Get to the data root
        parser.nextTag();

        //TODO: We need to overwrite any matching records here.
        TreeElement root = new TreeElementParser(parser, 0, fixtureId).parse();
        FormInstance instance = new FormInstance(root, fixtureId);

        //This is a terrible hack and clayton should feeel terrible about it
        if (userId != null) {
            instance.schema = userId;
        }

        //If we're using storage, deal properly
        if (storage() != null) {
            int recordId = -1;
            Vector<Integer> matchingFixtures = storage().getIDsForValue(FormInstance.META_ID, fixtureId);
            if (matchingFixtures.size() > 0) {
                //find all fixtures with the same user
                Vector<Integer> matchingUsers = storage().getIDsForValue(FormInstance.META_XMLNS, ExtUtil.emptyIfNull(userId));
                for (Integer i : matchingFixtures) {
                    if (matchingUsers.indexOf(i) != -1) {
                        recordId = i.intValue();
                    }
                }
            }

            if (recordId != -1) {
                if (!overwrite) {
                    //parse it out, but don't write anything to memory if one already exists
                    return instance;
                }
                instance.setID(recordId);
            }
        }

        commit(instance);

        return instance;
    }

    public void commit(FormInstance parsed) throws StorageFullException {
        storage().write(parsed);
    }

    public IStorageUtilityIndexed<FormInstance> storage() {
        //...ok... So. This is _not good_. It's badly written and redundant in a lot of ways.
        //the issue is that there are about 4 ways to set/override how this gets here
        //TODO: Fix this
        if (storage == null) {
            storage = (IStorageUtilityIndexed)StorageManager.getStorage("fixture");
        }
        return storage;
    }
}

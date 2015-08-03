package org.commcare.api.util;

import org.commcare.api.models.CommCareTransactionParserFactory;
import org.commcare.api.persistence.SqlSandbox;
import org.commcare.data.xml.DataModelPullParser;
import org.commcare.suite.model.User;
import org.javarosa.core.api.NameHasher;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Vector;

/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * TODO: We should try to centralize how these are used.
 *
 * @author ctsims
 */
public class UserDataUtils {

    public static SqlSandbox getStaticStorage(String username) {
        PrototypeFactory factory = new PrototypeFactory(new NameHasher());
        return new SqlSandbox(factory, username);
    }

    public static SqlSandbox getClearedStaticStorage(String username) {
        PrototypeFactory factory = new PrototypeFactory(new NameHasher());
        return new SqlSandbox(factory, username, true);
    }

    public static void clearStaticStorage(String username){
        SqlSandbox storage = getStaticStorage(username);
        storage.clearTables();
    }

    public static void parseXMLIntoSandbox(String restore, SqlSandbox sandbox) {
        InputStream stream = new ByteArrayInputStream(restore.getBytes(StandardCharsets.UTF_8));
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseFileIntoSandbox(File restore, SqlSandbox sandbox) throws FileNotFoundException {
        InputStream stream = new FileInputStream(restore);
        parseIntoSandbox(stream, sandbox);
    }

    public static void parseIntoSandbox(InputStream stream, SqlSandbox sandbox) {
        CommCareTransactionParserFactory factory = new CommCareTransactionParserFactory(sandbox);
        try {
            DataModelPullParser parser = new DataModelPullParser(stream, factory);
            parser.parse();
            sandbox.updateLastSync();
        } catch (IOException | UnfullfilledRequirementsException |
                XmlPullParserException | InvalidStructureException ioe) {
            ioe.printStackTrace();
        }
    }

    public static Date getLastSync(String username){
        SqlSandbox mSandbox = getStaticStorage(username);
        return mSandbox.getLastSync();
    }

    /**
     * For the users and groups in the provided sandbox, extracts out the list
     * of valid "owners" for entities (cases, ledgers, etc) in the universe.
     *
     * Borrowed from Android implementation, should likely be centralized.
     *
     * TODO: Move this static functionality into CommCare generally.
     */
    public static Vector<String> extractEntityOwners(SqlSandbox sandbox) {
        Vector<String> owners = new Vector<String>();
        Vector<String> users = new Vector<String>();

        for (IStorageIterator<User> userIterator = sandbox.getUserStorage().iterate(); userIterator.hasMore(); ) {
            String id = userIterator.nextRecord().getUniqueId();
            owners.addElement(id);
            users.addElement(id);
        }

        //Now add all of the relevant groups
        //TODO: Wow. This is.... kind of megasketch
        for (String userId : users) {
            DataInstance instance = loadFixture(sandbox, "user-groups", userId);
            if (instance == null) {
                continue;
            }
            EvaluationContext ec = new EvaluationContext(instance);
            for (TreeReference ref : ec.expandReference(XPathReference.getPathExpr("/groups/group/@id").getReference())) {
                AbstractTreeElement<AbstractTreeElement> idelement = ec.resolveReference(ref);
                if (idelement.getValue() != null) {
                    owners.addElement(idelement.getValue().uncast().getString());
                }
            }
        }

        return owners;
    }

    /**
     * Load the referenced fixture out of storage for the provided user
     *
     * @param sandbox
     * @param refId
     * @param userId
     * @return
     */
    public static FormInstance loadFixture(SqlSandbox sandbox,
                                            String refId, String userId) {
        IStorageUtilityIndexed<FormInstance> userFixtureStorage =
                sandbox.getUserFixtureStorage();
        IStorageUtilityIndexed<FormInstance> appFixtureStorage =
                sandbox.getAppFixtureStorage();

        Vector<Integer> userFixtures =
                userFixtureStorage.getIDsForValue(FormInstance.META_ID, refId);
        // ... Nooooot so clean.
        if (userFixtures.size() == 1) {
            // easy case, one fixture, use it
            return userFixtureStorage.read(userFixtures.elementAt(0).intValue());
            // TODO: Userid check anyway?
        } else if (userFixtures.size() > 1) {
            // intersect userid and fixtureid set.
            // TODO: Replace context call here with something from the session,
            // need to stop relying on that coupling
            Vector<Integer> relevantUserFixtures =
                    userFixtureStorage.getIDsForValue(FormInstance.META_XMLNS, userId);

            if (relevantUserFixtures.size() != 0) {
                Integer userFixture =
                        ArrayUtilities.intersectSingle(userFixtures, relevantUserFixtures);
                if (userFixture != null) {
                    return userFixtureStorage.read(userFixture.intValue());
                }
            }
        }

        // ok, so if we've gotten here there were no fixtures for the user,
        // let's try the app fixtures.
        Vector<Integer> appFixtures = appFixtureStorage.getIDsForValue(FormInstance.META_ID, refId);
        Integer globalFixture =
                ArrayUtilities.intersectSingle(appFixtureStorage.getIDsForValue(FormInstance.META_XMLNS, ""), appFixtures);
        if (globalFixture != null) {
            return appFixtureStorage.read(globalFixture.intValue());
        } else {
            // See if we have one manually placed in the suite
            Integer userFixture =
                    ArrayUtilities.intersectSingle(appFixtureStorage.getIDsForValue(FormInstance.META_XMLNS, userId), appFixtures);
            if (userFixture != null) {
                return appFixtureStorage.read(userFixture.intValue());
            }
            // Otherwise, nothing
            return null;
        }
    }
}

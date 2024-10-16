package org.commcare.core.sandbox;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
import org.javarosa.core.model.User;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.model.xform.XPathReference;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by wpride1 on 8/11/15.
 */
public class SandboxUtils {

    /**
     * A quick way to request an evaluation context with an abstract instance available.
     *
     * Used in Touchforms
     */
    @SuppressWarnings("unused")
    public static EvaluationContext getInstanceContexts(UserSandbox sandbox, String instanceId, String instanceRef){
        InstanceInitializationFactory iif = new CommCareInstanceInitializer(sandbox);

        Hashtable<String, DataInstance> instances = new Hashtable<>();
        ExternalDataInstance edi = new ExternalDataInstance(instanceRef, instanceId);
        edi.initialize(iif, instanceId);
        instances.put(instanceId, edi);

        return new EvaluationContext(null, instances);
    }

    /**
     * Load the referenced fixture out of storage for the provided user
     *
     * @param sandbox The current user's sandbox
     * @param refId The jr:// reference
     * @param userId The user's ID
     * @param appFixtureStorage Optionally, override the location to look for app fixtures
     * @return The form instance matching the refId in the sandbox
     */
    public static FormInstance loadFixture(UserSandbox sandbox,
                                           String refId,
                                           String userId,
                                           IStorageUtilityIndexed<FormInstance> appFixtureStorage) {
        IStorageUtilityIndexed<FormInstance> userFixtureStorage =
                sandbox.getUserFixtureStorage();

        Vector<Integer> userFixtures =
                userFixtureStorage.getIDsForValue(FormInstance.META_ID, refId);
        if (userFixtures.size() == 1) {
            return userFixtureStorage.read(userFixtures.elementAt(0));
            // TODO: Userid check anyway?
        } else if (userFixtures.size() > 1) {
            FormInstance result = intersectFixtureSets(userFixtureStorage, userId, userFixtures);
            if (result != null) {
                return result;
            }
        }

        if (appFixtureStorage != null) {
            FormInstance result = loadAppFixture(appFixtureStorage, refId, userId);
            if (result != null) {
                return result;
            }
        }
        return loadAppFixture(sandbox, refId, userId);
    }

    public static FormInstance loadFixture(UserSandbox sandbox, String refId, String userId) {
        return loadFixture(sandbox, refId, userId, null);
    }

    private static FormInstance intersectFixtureSets(IStorageUtilityIndexed<FormInstance> userFixtureStorage,
                                                     String userId,
                                                     Vector<Integer> userFixtures) {
        Vector<Integer> relevantUserFixtures =
                userFixtureStorage.getIDsForValue(FormInstance.META_XMLNS, userId);

        if (!relevantUserFixtures.isEmpty()) {
            Integer userFixture =
                    ArrayUtilities.intersectSingle(userFixtures, relevantUserFixtures);
            if (userFixture != null) {
                return userFixtureStorage.read(userFixture);
            }
        }
        return null;
    }

    private static FormInstance loadAppFixture(UserSandbox sandbox, String refId, String userId) {
        IStorageUtilityIndexed<FormInstance> appFixtureStorage =
                sandbox.getAppFixtureStorage();
        return loadAppFixture(appFixtureStorage, refId, userId);
    }

    private static FormInstance loadAppFixture(IStorageUtilityIndexed<FormInstance> appFixtureStorage, String refId, String userId) {
        Vector<Integer> appFixtures = appFixtureStorage.getIDsForValue(FormInstance.META_ID, refId);
        Integer globalFixture =
                ArrayUtilities.intersectSingle(appFixtureStorage.getIDsForValue(FormInstance.META_XMLNS, ""), appFixtures);
        if (globalFixture != null) {
            return appFixtureStorage.read(globalFixture);
        } else {
            // See if we have one manually placed in the suite
            Integer userFixture =
                    ArrayUtilities.intersectSingle(appFixtureStorage.getIDsForValue(FormInstance.META_XMLNS, userId), appFixtures);
            if (userFixture != null) {
                return appFixtureStorage.read(userFixture);
            }
            // Otherwise, nothing
            return null;
        }
    }

    /**
     * For the users and groups in the provided sandbox, extracts out the list
     * of valid "owners" for entities (cases, ledgers, etc) in the universe.
     */
    public static Vector<String> extractEntityOwners(UserSandbox sandbox) {
        Vector<String> owners = new Vector<>();
        Vector<String> users = new Vector<>();

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
                AbstractTreeElement idElement = ec.resolveReference(ref);
                if (idElement.getValue() != null) {
                    owners.addElement(idElement.getValue().uncast().getString());
                }
            }
        }

        return owners;
    }
}

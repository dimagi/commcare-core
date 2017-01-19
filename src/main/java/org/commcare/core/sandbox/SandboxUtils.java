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
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.model.xform.XPathReference;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by wpride1 on 8/11/15.
 */
public class SandboxUtils {
    /**
     * For the users and groups in the provided sandbox, extracts out the list
     * of valid "owners" for entities (cases, ledgers, etc) in the universe.
     *
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
     * @return The form instance matching the refId in the sandbox
     */
    public static FormInstance loadFixture(UserSandbox sandbox,
                                            String refId, String userId) {
        IStorageUtilityIndexed<FormInstance> userFixtureStorage =
                sandbox.getUserFixtureStorage();
        IStorageUtilityIndexed<FormInstance> appFixtureStorage = (IStorageUtilityIndexed<FormInstance>) StorageManager.getStorage("AppFixture");

        Vector<Integer> userFixtures =
                userFixtureStorage.getIDsForValue(FormInstance.META_ID, refId);
        // ... Nooooot so clean.
        if (userFixtures.size() == 1) {
            // easy case, one fixture, use it
            return userFixtureStorage.read(userFixtures.elementAt(0));
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
                    return userFixtureStorage.read(userFixture);
                }
            }
        }

        // ok, so if we've gotten here there were no fixtures for the user,
        // let's try the app fixtures.
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
}

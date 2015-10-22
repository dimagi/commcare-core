package org.commcare.util.mocks;

import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import java.util.Hashtable;
import java.util.Vector;


/**
 * Methods that mostly are used around the mocks that replicate stuff from
 * other projects.
 *
 * TODO: We should try to centralize how these are used.
 *
 * @author ctsims
 */
public class MockDataUtils {

    public static MockUserDataSandbox getStaticStorage() {
        PrototypeFactory factory = new PrototypeFactory(new ClassNameHasher());
        return new MockUserDataSandbox(factory);
    }

    /**
     * Load the referenced fixture out of storage for the provided user
     */
    public static FormInstance loadFixture(MockUserDataSandbox sandbox,
                                            String refId, String userId) {
        IStorageUtilityIndexed<FormInstance> userFixtureStorage =
                sandbox.getUserFixtureStorage();

        IStorageUtilityIndexed<FormInstance> appFixtureStorage = null;
        //this isn't great but generally this initialization path is actually
        //really hard/unclear for now, and we can't really assume the sandbox owns
        //this because it's app data, not user data.
        try {
            appFixtureStorage =
                    (IStorageUtilityIndexed)StorageManager.getStorage("fixture");
        } catch(RuntimeException re) {
            //We use this in some contexsts with app fixture storage and some without, so
            //if we don't find it, that's ok.
            //This behavior will need to get updated if this code is used outside of the util/test
            //context
        }

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

        //First see if app storage is even available, if not, we aren't gonna find one
        if(appFixtureStorage == null) {
            return null;
        }

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

    /**
     * Create an evaluation context with an abstract instance available.
     */
    public static EvaluationContext buildContextWithInstance(MockUserDataSandbox sandbox, String instanceId, String instanceRef){
        Hashtable<String, String> instanceRefToId = new Hashtable<>();
        instanceRefToId.put(instanceRef, instanceId);
        return buildContextWithInstances(sandbox, instanceRefToId);
    }

    /**
     * Create an evaluation context with an abstract instances available.
     */
    public static EvaluationContext buildContextWithInstances(MockUserDataSandbox sandbox,
                                                              Hashtable<String, String> instanceRefToId) {
        InstanceInitializationFactory iif = new CLIInstanceInitializer(sandbox);

        Hashtable<String, DataInstance> instances = new Hashtable<>();
        for (String instanceRef : instanceRefToId.keySet()) {
            String instanceId = instanceRefToId.get(instanceRef);
            ExternalDataInstance edi = new ExternalDataInstance(instanceRef, instanceId);

            instances.put(instanceId, edi.initialize(iif, instanceId));
        }

        return new EvaluationContext(null, instances);
    }
}

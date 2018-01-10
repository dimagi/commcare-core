package org.commcare.util.mocks;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.core.process.CommCareInstanceInitializer;
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
     * Create an evaluation context with an abstract instance available.
     */
    public static EvaluationContext buildContextWithInstance(UserSandbox sandbox, String instanceId, String instanceRef){
        Hashtable<String, String> instanceRefToId = new Hashtable<>();
        instanceRefToId.put(instanceRef, instanceId);
        return buildContextWithInstances(sandbox, instanceRefToId);
    }

    /**
     * Create an evaluation context with an abstract instances available.
     */
    public static EvaluationContext buildContextWithInstances(UserSandbox sandbox,
                                                              Hashtable<String, String> instanceRefToId) {
        InstanceInitializationFactory iif = new CommCareInstanceInitializer(sandbox);

        Hashtable<String, DataInstance> instances = new Hashtable<>();
        for (String instanceRef : instanceRefToId.keySet()) {
            String instanceId = instanceRefToId.get(instanceRef);
            ExternalDataInstance edi = new ExternalDataInstance(instanceRef, instanceId);

            instances.put(instanceId, edi.initialize(iif, instanceId));
        }

        return new EvaluationContext(null, instances);
    }
}

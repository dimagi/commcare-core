package org.javarosa.core.model.instance;

import org.javarosa.core.services.Logger;

import java.util.Hashtable;

/**
 * Dispatchs on instance ID to create new external data instances.  Useful for
 * injecting custom functionality for specific data instances, for instance if
 * you want casedb template resolution to behave different than normal external
 * instance resolution.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class ExternalDataInstanceFactory {
    private static final Hashtable<String, ExternalDataInstanceBuilder> instanceIdToBuilder =
            new Hashtable<String, ExternalDataInstanceBuilder>();
    private static final Object lock = new Object();
    private static final ExternalDataInstance dummyDefaultInstance =
            new ExternalDataInstance();

    /**
     * Builds a new external data instance using the instance builder
     * registered to the given instanceId.  Defaults to building an
     * ExternalDataInstance instnace if no builder is registered under the
     * provided instanceId.
     */
    public static ExternalDataInstance getNewExternalDataInstance(String instanceId,
                                                                  String dataReference) {
        if (instanceIdToBuilder.containsKey(instanceId)) {
            ExternalDataInstanceBuilder builder = instanceIdToBuilder.get(instanceId);
            return builder.buildExternalDataInstance(dataReference, instanceId);
        } else {
            return dummyDefaultInstance.buildExternalDataInstance(dataReference, instanceId);
        }
    }

    /**
     * Register that instances of a given id should be represented with
     * particular external data instance class
     */
    public static void registerExternalDataInstanceBuilder(String instanceId,
                                                           ExternalDataInstanceBuilder instanceBuilder) {
        synchronized (lock) {
            if (instanceIdToBuilder.contains(instanceId)) {
                Logger.log("Warning", "registering an existing external data instance");
            }
            instanceIdToBuilder.put(instanceId, instanceBuilder);
        }
    }
}

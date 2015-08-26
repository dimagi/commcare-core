package org.javarosa.core.model.instance;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class ExternalDataInstanceFactory {
    private final Hashtable<String, DataInstanceBuilder> instanceIdToBuilder;

    public ExternalDataInstanceFactory() {
        instanceIdToBuilder = new Hashtable<String, DataInstanceBuilder>();
    }

    public ExternalDataInstance getDataInstance(String instanceId, String reference) {
        if (instanceIdToBuilder.contains(instanceId)) {
            DataInstanceBuilder builder = instanceIdToBuilder.get(instanceId);
            return builder.buildDataInstance(reference, instanceId);
        } else {
            return new ExternalDataInstance(reference, instanceId);
        }
    }

    public void registerInstanceBuilder(String instanceId, DataInstanceBuilder builder) {
        instanceIdToBuilder.put(instanceId, builder);
    }
}

package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.ExternalDataInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In memory implementation of VirtualDataInstanceStorage for use in the CLI and tests.
 */
public class MemoryVirtualDataInstanceStorage implements
        VirtualDataInstanceStorage {

    private Map<String, ExternalDataInstance> storage = new HashMap<>();

    @Override
    public String write(ExternalDataInstance dataInstance) {
        String key = UUID.randomUUID().toString();
        storage.put(key, dataInstance);
        return key;
    }

    @Override
    public ExternalDataInstance read(String key) {
        return storage.get(key);
    }

    @Override
    public boolean contains(String key) {
        return storage.containsKey(key);
    }
}

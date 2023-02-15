package org.commcare.core.interfaces;

import org.javarosa.core.model.instance.ExternalDataInstance;

/**
 * Read and write operations for entity selections made on a mult-select Entity Screen
 */
public interface VirtualDataInstanceStorage {
    String write(ExternalDataInstance dataInstance);

    String write(String key, ExternalDataInstance dataInstance);

    /**
     * Load an instance from storage.
     *
     * @param key The instance storage key
     * @param instanceId The instanceId to apply to the loaded instance
     * @param refId Unique reference id to apply to the loaded instance
     */
    ExternalDataInstance read(String key, String instanceId, String refId);

    boolean contains(String key);
}

package org.commcare.cases.entity;

import org.commcare.suite.model.Detail;

import java.io.Closeable;
import java.util.Hashtable;

/**
 * Interface for evaluated entity fields cache
 */
public interface EntityStorageCache {
    enum ValueType {
        TYPE_NORMAL_FIELD,
        TYPE_SORT_FIELD
    }

    Closeable lockCache();

    String getCacheKey(String detailId, String detailFieldIndex, ValueType valueType);

    String retrieveCacheValue(String cacheIndex, String cacheKey);

    void cache(String cacheIndex, String cacheKey, String data);

    int getFieldIdFromCacheKey(String detailId, String cacheKey);

    void primeCache(Hashtable<String, AsyncEntity> entitySet, String[][] cachePrimeKeys, Detail detail);
}

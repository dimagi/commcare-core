package org.commcare.cases.entity;

import org.commcare.suite.model.Detail;

import java.util.Hashtable;

/**
 * Interface for evaluated entity fields cache
 */
public interface EntityStorageCache {
    boolean lockCache();

    void releaseCache();

    String getCacheKey(String detailId, String detailFieldIndex);

    String retrieveCacheValue(String cacheIndex, String cacheKey);

    void cache(String cacheIndex, String cacheKey, String data);

    int getSortFieldIdFromCacheKey(String detailId, String cacheKey);

    void primeCache(Hashtable<String, AsyncEntity> entitySet, String[][] cachePrimeKeys, Detail detail);
}

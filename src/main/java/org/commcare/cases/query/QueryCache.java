package org.commcare.cases.query;

import java.util.HashMap;

/**
 * Created by ctsims on 1/26/2017.
 */

public class QueryCache {

    HashMap<Class, QueryCacheEntry> cacheEntries = new HashMap<>();
    QueryCache parent;

    public QueryCache() {

    }

    public QueryCache(QueryCache parent) {
        this.parent = parent;
    }

    public <T extends QueryCacheEntry> T getQueryCache(Class<T> cacheType) {
        T t = getQueryCacheOrNull(cacheType);
        if(t != null) {
            return t;
        }
        try {
            t = cacheType.newInstance();
            cacheEntries.put(cacheType, t);
            return t;
        } catch (InstantiationException e) {
            throw new RuntimeException("Couldn't create cache " + cacheType, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't create cache " + cacheType, e);
        }
    }

    public <T extends QueryCacheEntry> T getQueryCacheOrNull(Class<T> cacheType) {
        if (cacheEntries.containsKey(cacheType)) {
            return (T)cacheEntries.get(cacheType);
        } else if (parent != null) {
            return parent.getQueryCacheOrNull(cacheType);
        } else {
            return null;
        }
    }
}

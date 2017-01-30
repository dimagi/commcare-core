package org.commcare.cases.query.handlers;

import org.commcare.cases.query.*;
import org.commcare.cases.query.IndexedValueLookup;
import org.commcare.cases.query.PredicateProfile;
import org.commcare.cases.util.LruCache;
import org.commcare.cases.util.QueryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class BasicStorageBackedCachingQueryHandler implements org.commcare.cases.query.QueryHandler<org.commcare.cases.query.IndexedValueLookup> {
    HashMap<String, LruCache<Object, List<Integer>>> caches = new HashMap<>();

    @Override
    public int getExpectedRuntime() {
        return 10;
    }

    @Override
    public org.commcare.cases.query.IndexedValueLookup profileHandledQuerySet(Vector<org.commcare.cases.query.PredicateProfile> profiles) {
        org.commcare.cases.query.IndexedValueLookup ret = QueryUtils.getFirstKeyIndexedValue(profiles);
        if(ret != null){
            if(caches.containsKey(ret.getKey())) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public List<Integer> loadProfileMatches(IndexedValueLookup querySet, QueryContext queryContext) {
        LruCache<Object, List<Integer>> cache = caches.get(querySet.getKey());
        if(cache == null) {
            return null;
        }

        List<Integer> potentialResult = cache.get(querySet.value);
        return potentialResult;
    }

    @Override
    public void updateProfiles(IndexedValueLookup querySet, Vector<PredicateProfile> profiles) {
        profiles.remove(querySet);
    }

    public void cacheResult(String key, Object value, List<Integer> results) {
        LruCache<Object, List<Integer>> cache;
        if(!caches.containsKey(key)) {
            cache = new LruCache<>(10);
            caches.put(key, cache);
        } else {
            cache = caches.get(key);
        }
        cache.put(value, results);
    }
}

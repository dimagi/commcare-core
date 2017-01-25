package org.commcare.cases.util;

import org.javarosa.xpath.expr.FunctionUtils;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class BasicStorageBackedCachingQueryHandler implements QueryHandler<IndexedValueLookup> {
    HashMap<String, LruCache<Object, Vector<Integer>>> caches = new HashMap<>();

    @Override
    public int getExpectedRuntime() {
        return 10;
    }

    @Override
    public IndexedValueLookup profileHandledQuerySet(Vector<PredicateProfile> profiles) {
        IndexedValueLookup ret = QueryUtils.getFirstKeyIndexedValue(profiles);
        if(ret != null){
            if(caches.containsKey(ret.getKey())) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public Vector<Integer> loadProfileMatches(IndexedValueLookup querySet) {
        LruCache<Object, Vector<Integer>> cache = caches.get(querySet.getKey());
        if(cache == null) {
            return null;
        }

        Vector<Integer> potentialResult = cache.get(querySet.value);
        return potentialResult;
    }

    @Override
    public void updateProfiles(IndexedValueLookup querySet, Vector<PredicateProfile> profiles) {
        profiles.remove(querySet);
    }

    public void cacheResult(String key, Object value, Vector<Integer> results) {
        LruCache<Object, Vector<Integer>> cache;
        if(!caches.containsKey(key)) {
            cache = new LruCache<>(10);
            caches.put(key, cache);
        } else {
            cache = caches.get(key);
        }
        cache.put(value, results);
    }
}

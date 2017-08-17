package org.commcare.modern.engine.cases;

import org.commcare.cases.model.Case;
import org.commcare.cases.query.QueryCache;

import java.util.HashMap;

/**
 * A straightforward cache object query cache. Stores objects by their record ID.
 *
 * Used by other optimizations to isolate doing bulk loads and ensure that they are relevant
 * when they occur
 *
 * Created by ctsims on 6/22/2017.
 */

public class RecordObjectCache<T> implements QueryCache {

    private HashMap<String,HashMap<Integer, T>> caches = new HashMap<>();

    public boolean isLoaded(String storageSetID, int recordId) {
        return getCache(storageSetID).containsKey(recordId);
    }

    public HashMap<Integer, T> getLoadedCaseMap(String storageSetID) {
        return getCache(storageSetID);
    }

    public T getLoadedRecordObject(String storageSetID, int recordId) {
        return getCache(storageSetID).get(recordId);
    }

    private HashMap<Integer, T> getCache(String storageSetID) {
        HashMap<Integer, T> cache;
        if(!caches.containsKey(storageSetID)) {
            cache = new HashMap<>();
            caches.put(storageSetID, cache);
        } else {
            cache = caches.get(storageSetID);
        }
        return cache;
    }
}

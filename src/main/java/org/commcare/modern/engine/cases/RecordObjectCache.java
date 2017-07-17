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

    private HashMap<Integer, T> cachedRecordObjects = new HashMap<>();

    public boolean isLoaded(int recordId) {
        return cachedRecordObjects.containsKey(recordId);
    }

    public HashMap<Integer, T> getLoadedCaseMap() {
        return cachedRecordObjects;
    }

    public T getLoadedRecordObject(int recordId) {
        return cachedRecordObjects.get(recordId);
    }
}

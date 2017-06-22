package org.commcare.modern.engine.cases;

import org.commcare.cases.model.Case;
import org.commcare.cases.query.QueryCache;

import java.util.HashMap;

/**
 * A straightforward cache object query cache. Stores cases by their record ID.
 *
 * Used by other optimizations to isolate doing bulk loads and ensure that they are relevant
 * when they occur
 *
 * Created by ctsims on 6/22/2017.
 */

public class CaseObjectCache implements QueryCache {

    private HashMap<Integer, Case> cachedCases = new HashMap<>();

    public boolean isLoaded(int recordId) {
        return cachedCases.containsKey(recordId);
    }

    public HashMap<Integer, Case> getLoadedCaseMap() {
        return cachedCases;
    }

    public Case getLoadedCase(int recordId) {
        return cachedCases.get(recordId);
    }
}

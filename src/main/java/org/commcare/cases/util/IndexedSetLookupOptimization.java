package org.commcare.cases.util;

/**
 * Created by ctsims on 1/18/2017.
 */

public class IndexedSetLookupOptimization implements PredicateEvaluationOptimization {
    public final String key;
    public final String[] valueSet;

    public IndexedSetLookupOptimization(String key, Object valueSet ) {
        this.key = key;
        this.valueSet = ((String)valueSet).split(" ");
    }

    public String getKey() {
        return key;
    }
}

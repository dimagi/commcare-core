package org.commcare.cases.util;

/**
 * Created by ctsims on 1/18/2017.
 */

public class IndexedValueLookupOptimization implements PredicateEvaluationOptimization {
    public final String key;
    public final Object value;

    public IndexedValueLookupOptimization(String key, Object value ) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
}

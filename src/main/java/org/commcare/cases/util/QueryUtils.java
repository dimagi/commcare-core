package org.commcare.cases.util;

import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class QueryUtils {
    static IndexedValueLookup getFirstKeyIndexedValue(Vector<PredicateProfile> profiles) {
        if(profiles.elementAt(0) instanceof IndexedValueLookup) {
            return (IndexedValueLookup)profiles.elementAt(0);
        }
        return null;
    }

    public static Vector<Integer> wrapSingleResult(Integer result) {
        Vector<Integer> results = new Vector<>();
        results.add(result);
        return results;
    }
}

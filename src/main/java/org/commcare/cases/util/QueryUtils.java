package org.commcare.cases.util;

import org.commcare.cases.query.IndexedValueLookup;

import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class QueryUtils {
    public static org.commcare.cases.query.IndexedValueLookup getFirstKeyIndexedValue(Vector<org.commcare.cases.query.PredicateProfile> profiles) {
        if(profiles.elementAt(0) instanceof org.commcare.cases.query.IndexedValueLookup) {
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

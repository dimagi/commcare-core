package org.commcare.cases.util;

import org.commcare.cases.query.IndexedValueLookup;
import org.commcare.cases.query.PredicateProfile;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QuerySensitive;

import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class QueryUtils {
    public static IndexedValueLookup getFirstKeyIndexedValue(Vector<PredicateProfile> profiles) {
        if (profiles.elementAt(0) instanceof IndexedValueLookup) {
            return (IndexedValueLookup)profiles.elementAt(0);
        }
        return null;
    }

    public static Vector<Integer> wrapSingleResult(Integer result) {
        Vector<Integer> results = new Vector<>();
        results.add(result);
        return results;
    }

    /**
     * If the provided object has the QuerySensitive instance tag, provide the object with the
     * current query context so it can potentially prepare itself for use in a more efficient
     * manner
     */
    public static void prepareSensitiveObjectForUseInCurrentContext(Object o, QueryContext context) {
        if (o instanceof QuerySensitive) {
            ((QuerySensitive)o).prepareForUseInCurrentContext(context);
        }
    }
}

package org.commcare.modern.engine.cases;

import org.commcare.cases.model.Case;
import org.commcare.cases.query.QueryCache;
import org.commcare.modern.util.Pair;

import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * A case group result cache keeps track of different sets of "Bulk" cases which are
 * likely to have data or operations tracked about them (IE: results of a common query which
 * are likely to have further filtering applied.
 *
 * Since these results are often captured/reported before a context is escalated, this cache
 * doesn't directly hold the resulting cached cases themselves. Rather a CaseObjectCache should
 * be used to track the resulting cases. This will ensure that cache can be attached to the
 * appropriate lifecycle
 *
 * Created by ctsims on 1/25/2017.
 */

public class CaseGroupResultCache implements QueryCache {

    private HashMap<String,LinkedHashSet<Integer>> bulkFetchBodies = new HashMap<>();

    public void reportBulkCaseBody(String key, LinkedHashSet<Integer> ids) {
        if(bulkFetchBodies.containsKey(key)) {
            return;
        }
        bulkFetchBodies.put(key, ids);
    }

    public boolean hasMatchingCaseSet(int recordId) {
        return getTranche(recordId) != null;
    }

    public Pair<String, LinkedHashSet<Integer>> getTranche(int recordId) {
        for(String key : bulkFetchBodies.keySet()) {
            LinkedHashSet<Integer> tranche = bulkFetchBodies.get(key);
            if(tranche.contains(recordId)){
                return new Pair<>(key, tranche);
            }
        }
        return null;
    }
}

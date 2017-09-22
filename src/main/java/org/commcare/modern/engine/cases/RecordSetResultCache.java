package org.commcare.modern.engine.cases;

import org.commcare.cases.query.QueryCache;
import org.commcare.modern.util.Pair;

import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * A record set result cache keeps track of different sets of "Bulk" record which are
 * likely to have data or operations tracked about them (IE: results of a common query which
 * are likely to have further filtering applied.)
 *
 * Since these results are often captured/reported before a context is escalated, this cache
 * doesn't directly hold the resulting cached records themselves. Rather a RecordObjectCache should
 * be used to track the resulting records. This will ensure that cache can be attached to the
 * appropriate lifecycle
 *
 * Created by ctsims on 1/25/2017.
 */

public class RecordSetResultCache implements QueryCache {

    private HashMap<String,Pair<String, LinkedHashSet<Integer>>> bulkFetchBodies = new HashMap<>();

    /**
     * Report a set of bulk records that are likely to be needed as a group.
     *
     * @param key A unique key for the provided record set. It is presumed that if the key is
     *            already in use that the id set is redundant.
     * @param storageSetID The name of the Storage where the records are stored.
     * @param ids The record set ID's
     */
    public void reportBulkRecordSet(String key, String storageSetID, LinkedHashSet<Integer> ids) {
        String fullKey = key +"|" + storageSetID;
        if (bulkFetchBodies.containsKey(fullKey)) {
            return;
        }
        bulkFetchBodies.put(fullKey, new Pair<>(storageSetID, ids));
    }

    public boolean hasMatchingRecordSet(String recordSetName, int recordId) {
        return getRecordSetForRecordId(recordSetName, recordId) != null;
    }

    public Pair<String, LinkedHashSet<Integer>> getRecordSetForRecordId(String recordSetName,
                                                                        int recordId) {
        Pair<String, LinkedHashSet<Integer>> match = null;
        for (String key : bulkFetchBodies.keySet()) {
            Pair<String, LinkedHashSet<Integer>> tranche = bulkFetchBodies.get(key);
            if (tranche.second.contains(recordId) && tranche.first.equals(recordSetName)) {
                if(match == null || (tranche.second.size() < match.second.size())) {
                    match = new Pair<>(key, tranche.second);
                }
            }
        }
        return match;
    }
}

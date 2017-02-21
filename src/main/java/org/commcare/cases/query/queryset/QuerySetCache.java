package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ctsims on 2/6/2017.
 */

public class QuerySetCache implements QueryCache {

    private Map<String, ModelQuerySet> querySetMap = new HashMap<>();

    public ModelQuerySet getModelQuerySet(String querySetId) {
        return querySetMap.get(querySetId);
    }

    public void addModelQuerySet(String querySetId, ModelQuerySet set) {
        querySetMap.put(querySetId, set);
    }
}

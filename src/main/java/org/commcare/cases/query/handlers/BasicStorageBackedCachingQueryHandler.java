package org.commcare.cases.query.handlers;

import org.commcare.cases.query.*;
import org.commcare.cases.query.IndexedValueLookup;
import org.commcare.cases.query.PredicateProfile;
import org.commcare.cases.util.LruCache;
import org.commcare.cases.util.QueryUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class BasicStorageBackedCachingQueryHandler implements QueryHandler<IndexedValueLookup> {
    private HashMap<String, LruCache<Object, List<Integer>>> caches = new HashMap<>();

    @Override
    public int getExpectedRuntime() {
        return 10;
    }

    @Override
    public IndexedValueLookup profileHandledQuerySet(Vector<PredicateProfile> profiles) {
        IndexedValueLookup ret = QueryUtils.getFirstKeyIndexedValue(profiles);
        if (ret != null){
            if (caches.containsKey(ret.getKey())) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public List<Integer> loadProfileMatches(IndexedValueLookup querySet, QueryContext queryContext) {
        LruCache<Object, List<Integer>> cache = caches.get(querySet.getKey());
        if (cache == null) {
            return null;
        }

        return cache.get(querySet.value);
    }

    @Override
    public void updateProfiles(IndexedValueLookup querySet, Vector<PredicateProfile> profiles) {
        profiles.remove(querySet);
    }

    @Override
    public Collection<PredicateProfile> collectPredicateProfiles(Vector<XPathExpression> predicates,
                                                                 QueryContext context,
                                                                 EvaluationContext evaluationContext) {
        return null;
    }

    public void cacheResult(String key, Object value, List<Integer> results) {
        LruCache<Object, List<Integer>> cache;
        if (!caches.containsKey(key)) {
            cache = new LruCache<>(10);
            caches.put(key, cache);
        } else {
            cache = caches.get(key);
        }
        cache.put(value, results);
    }
}

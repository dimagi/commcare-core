package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryContext;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by ctsims on 2/6/2017.
 */

public abstract class DerivedCaseQueryLookup implements QuerySetLookup {

    protected QuerySetLookup root;

    public DerivedCaseQueryLookup(QuerySetLookup root) {
        this.root = root;
    }

    protected QuerySetLookup getRootLookup() {
        return root;
    }

    @Override
    public boolean isValid(TreeReference ref, QueryContext context) {
        return ref != null && root.isValid(ref, context);
    }

    @Override
    public TreeReference getLookupIdKey(EvaluationContext evaluationContext) {
        return root.getLookupIdKey(evaluationContext);
    }

    @Override
    public List<Integer> performSetLookup(TreeReference lookupIdKey, QueryContext queryContext) {
        List<Integer> rootLookup = root.performSetLookup(lookupIdKey, queryContext);
        ModelQuerySet set = getOrLoadCachedQuerySet(queryContext);

        List<Integer> returnSet = new Vector<>();
        for(Integer i : rootLookup) {
            returnSet.addAll(set.getMatchingValues(i));
        }
        return returnSet;
    }

    @Override
    public Set<Integer> getLookupSetBody(QueryContext queryContext) {
        ModelQuerySet set = getOrLoadCachedQuerySet(queryContext);
        return set.getSetBody();
    }


    private ModelQuerySet getOrLoadCachedQuerySet(QueryContext queryContext) {
        QuerySetCache cache = queryContext.getQueryCache(QuerySetCache.class);

        ModelQuerySet set = cache.getModelQuerySet(this.getCurrentQuerySetId());
        if(set == null) {
            set = loadModelQuerySet(queryContext);
            cache.addModelQuerySet(this.getCurrentQuerySetId(), set);
        }
        return set;
    }


    protected abstract ModelQuerySet loadModelQuerySet(QueryContext queryContext);
}

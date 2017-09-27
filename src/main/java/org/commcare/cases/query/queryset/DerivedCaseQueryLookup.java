package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryContext;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * A derived query lookup is the result of an operation with translates one set of cases to another.
 *
 * As an example, when running the query in a vacuum
 *
 * instance('casedb')/casedb/case
 *                  [@case_type='FOO'][@case_id = current()/index/parent][somevalue = 'blank']
 *
 * After the first predicate executes a model query set may be generated containing all matches,
 * that would be the "current()" model query set
 *
 * A QuerySet Transform could then produce a DerivedCaseQueryLookup based on the original query
 * set that instead of returning the current case's model id would instead produce the current
 * case's parent ID.
 *
 * To do this the implementing class will need to provide an implementation of the
 *
 * loadModelQuerySet()
 *
 * method. This method should take the provided root model set and generate its own model set based
 * on it. This class will handle the relevant caching and lookup of values into that model set
 *
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
        if(rootLookup == null) {
            return null;
        }
        ModelQuerySet set = getOrLoadCachedQuerySet(queryContext);
        if(set == null) {
            return null;
        }

        List<Integer> returnSet = new Vector<>();
        for(Integer i : rootLookup) {
            Collection<Integer> matching = set.getMatchingValues(i);
            if(matching == null) {
                return null;
            }
            returnSet.addAll(matching);
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

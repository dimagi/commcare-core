package org.commcare.cases.query.queryset;

import org.commcare.cases.query.QueryCacheEntry;
import org.commcare.cases.query.QueryContext;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Created by ctsims on 2/6/2017.
 */

public class CaseQuerySetLookup implements QuerySetLookup {
    public static final String CASE_MODEL_ID = "case";

    private TreeReference caseDbRoot;
    private Map<Integer, Integer> multiplicityMap;

    public CaseQuerySetLookup(TreeReference caseDbRoot, Map<Integer, Integer> multiplicityMap) {
        this.caseDbRoot = caseDbRoot;
        this.multiplicityMap = multiplicityMap;
    }

    @Override
    public boolean isValid(TreeReference ref, QueryContext context) {
        if(ref == null) {
            return false;
        } else {
            ModelQuerySet set =
                    context.getQueryCache(QuerySetCache.class).getModelQuerySet(CurrentModelQuerySet.CURRENT_QUERY_SET_ID);
            return set != null;
        }
    }

    @Override
    public String getQueryModelId() {
        return CASE_MODEL_ID;
    }

    @Override
    public String getCurrentQuerySetId() {
        return CurrentModelQuerySet.CURRENT_QUERY_SET_ID;
    }

    @Override
    public TreeReference getLookupIdKey(EvaluationContext evaluationContext) {
        TreeReference current = evaluationContext.getOriginalContext();
        if(current.size() < 1) {
            return null;
        }
        TreeReference generic = current.genericizeAfter(current.size() - 1);
        if(generic.equals(caseDbRoot)) {
            return current;
        }
        return null;
    }

    @Override
    public List<Integer> performSetLookup(TreeReference element, QueryContext queryContext) {
        Integer match = queryContext.getQueryCache(CaseQuerySetLookupCache.class).
                lookupQuerySetMatch(queryContext, element, multiplicityMap);
        if(match == null) {
            return null;
        }
        Vector<Integer> returnVal = new Vector<>();
        returnVal.add(match);
        return returnVal;
    }

    @Override
    public Set<Integer> getLookupSetBody(QueryContext queryContext) {
        return queryContext.getQueryCache(CaseQuerySetLookupCache.class).
                getLookupSetBody(queryContext, multiplicityMap);
    }

    public static class CaseQuerySetLookupCache implements QueryCacheEntry {
        Map<TreeReference, Integer> caseQueryIndex;
        Set<Integer> lookupSetBody;

        public Integer lookupQuerySetMatch(QueryContext context, TreeReference currentRef, Map<Integer, Integer> multiplicityMap) {
            if(caseQueryIndex == null) {
                loadCaseQuerySetCache(context, multiplicityMap);
            }
            return caseQueryIndex.get(currentRef);

        }

        private void loadCaseQuerySetCache(QueryContext context, Map<Integer, Integer> multiplicityMap) {
            CurrentModelQuerySet set = (CurrentModelQuerySet)context.
                    getQueryCache(QuerySetCache.class).
                    getModelQuerySet(CurrentModelQuerySet.CURRENT_QUERY_SET_ID);

            caseQueryIndex = new HashMap<>();
            lookupSetBody = new LinkedHashSet<>();
            for(TreeReference ref : set.getCurrentQuerySet()) {
                Integer mult = ref.getMultiplicity(ref.size()-1);
                Integer modelId = multiplicityMap.get(mult);
                caseQueryIndex.put(ref, modelId);
                lookupSetBody.add(modelId);
            }
        }

        public Set<Integer> getLookupSetBody(QueryContext context, Map<Integer, Integer> multiplicityMap) {
            if(caseQueryIndex == null) {
                loadCaseQuerySetCache(context, multiplicityMap);
            }
            return lookupSetBody;
        }
    }
}

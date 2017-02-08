package org.commcare.cases.query.handlers;

import org.commcare.cases.query.queryset.ModelQueryLookup;
import org.commcare.cases.query.queryset.ModelQuerySetMatcher;
import org.commcare.cases.query.PredicateProfile;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QueryHandler;
import org.commcare.cases.query.queryset.QuerySetLookup;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 *
 *
 * Created by ctsims on 1/31/2017.
 */

public class ModelQueryLookupHandler implements QueryHandler<ModelQueryLookup> {
    private ModelQuerySetMatcher matcher;

    public ModelQueryLookupHandler(ModelQuerySetMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public int getExpectedRuntime() {
        return 1;
    }

    @Override
    public ModelQueryLookup profileHandledQuerySet(Vector<PredicateProfile> profiles) {
        if(profiles.get(0) instanceof ModelQueryLookup) {
            return (ModelQueryLookup)profiles.get(0);
        }
        return null;
    }

    @Override
    public List<Integer> loadProfileMatches(ModelQueryLookup getLookupKey, QueryContext queryContext) {
        QuerySetLookup lookup = getLookupKey.getSetLookup();

        EvaluationTrace trace = new EvaluationTrace("QuerySetLookup|" + lookup.getCurrentQuerySetId());

        List<Integer> lookupData = lookup.
                performSetLookup(getLookupKey.getRootLookupRef(), queryContext);

        if(lookupData != null) {
            trace.setOutcome("Results: " +lookupData.size());
            queryContext.reportTrace(trace);
        }

        return lookupData;
    }

    @Override
    public void updateProfiles(ModelQueryLookup querySet, Vector<PredicateProfile> profiles) {
        profiles.removeElementAt(0);
    }

    @Override
    public Collection<PredicateProfile> collectPredicateProfiles(Vector<XPathExpression> predicates,
                                                                 QueryContext context,
                                                                 EvaluationContext evalContext) {

        QuerySetLookup lookup = matcher.getQueryLookupFromPredicate(predicates.elementAt(0));
        if(lookup == null) {
            return null;
        }

        TreeReference ref = lookup.getLookupIdKey(evalContext);

        if(!lookup.isValid(ref, context)) {
            return null;
        }

        Vector<PredicateProfile> newProfile = new Vector<>();
        newProfile.add(new ModelQueryLookup(lookup.getQueryModelId(), lookup, ref));
        return newProfile;
    }
}

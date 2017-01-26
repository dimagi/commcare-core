package org.commcare.cases.query.handlers;

import org.commcare.cases.query.IndexedValueLookup;
import org.commcare.cases.query.PredicateProfile;
import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.QueryPlanner;
import org.commcare.cases.util.QueryUtils;
import org.javarosa.xpath.expr.FunctionUtils;

import java.util.Vector;

/**
 * A static lookup handler is useful when a storage root keeps track of some data internally,
 * and is able to process requests for that data without needing additional work
 *
 * Created by ctsims on 1/25/2017.
 */

public abstract class StaticLookupQueryHandler implements org.commcare.cases.query.QueryHandler<org.commcare.cases.query.IndexedValueLookup> {

    protected abstract boolean canHandle(String key);
    protected abstract Vector<Integer> getMatches(String key, String valueToMatch);

    @Override
    public int getExpectedRuntime() {
        return 1;
    }

    @Override
    public org.commcare.cases.query.IndexedValueLookup profileHandledQuerySet(Vector<org.commcare.cases.query.PredicateProfile> profiles) {
        org.commcare.cases.query.IndexedValueLookup ret = QueryUtils.getFirstKeyIndexedValue(profiles);
        if(ret != null){
            if(canHandle(ret.getKey())) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public Vector<Integer> loadProfileMatches(org.commcare.cases.query.IndexedValueLookup querySet,
                                              QueryContext queryContext) {
        return getMatches(querySet.getKey(), FunctionUtils.toString((querySet).value));
    }

    @Override
    public void updateProfiles(IndexedValueLookup querySet, Vector<PredicateProfile> profiles) {
        profiles.remove(querySet);
    }

}

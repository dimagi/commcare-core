package org.commcare.cases.query;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class QueryPlanner {
    private List<QueryHandler> handlers = new Vector<>();

    /**
     * @param profiles note: Should remove profiles which have been handled
     *
     * @param currentQueryContext
     * @return null if the query could not be handled by this planner
     */
    public List<Integer> attemptProfiledQuery(Vector<PredicateProfile> profiles,
                                                QueryContext currentQueryContext){
        for(int i = 0 ; i < handlers.size() ; ++i) {
            QueryHandler handler = handlers.get(i);
            Object queryPlan = handler.profileHandledQuerySet(profiles);
            if(queryPlan != null) {
                List<Integer> retVal = handler.loadProfileMatches(queryPlan, currentQueryContext);
                if(retVal != null) {
                    handler.updateProfiles(queryPlan, profiles);
                    return retVal;
                }
            }
        }
        return null;
    }

    public void addQueryHandler(QueryHandler handler) {
        handlers.add(handler);
        Collections.sort(handlers, new Comparator<QueryHandler>() {
            @Override
            public int compare(QueryHandler first, QueryHandler second) {
                return first.getExpectedRuntime() - second.getExpectedRuntime();
            }
        });
    }


    public Collection<PredicateProfile> collectPredicateProfiles(
            Vector<XPathExpression> predicates, QueryContext context, EvaluationContext evalContext) {
        if(predicates == null) {
            return null;
        }
        Vector<PredicateProfile> returnProfile = new Vector<>();
        for (int i = 0; i < handlers.size(); ++i) {
            Collection<PredicateProfile> profile =
                    handlers.get(i).collectPredicateProfiles(predicates, context, evalContext);
            if(profile != null) {
                returnProfile.addAll(profile);
            }
        }
        return returnProfile;
    }


}

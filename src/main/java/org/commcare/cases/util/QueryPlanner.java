package org.commcare.cases.util;

import org.commcare.modern.util.Pair;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.trace.EvaluationTrace;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * Created by ctsims on 1/25/2017.
 */

public class QueryPlanner {
    private List<QueryHandler> handlers = new Vector<>();

    private EvaluationContext currentScope;

    private boolean queryModeAggressive = false;

    public List<QueryCue> aggressiveCues = new Vector<>();

    /**
     * @param profiles note: Should remove profiles which have been handled
     *
     * @return null if the query could not be handled by this planner
     */
    public Vector<Integer> attemptProfiledQuery(Vector<PredicateProfile> profiles){
        for(int i = 0 ; i < handlers.size() ; ++i) {
            QueryHandler handler = handlers.get(i);
            Object queryPlan = handler.profileHandledQuerySet(profiles);
            if(queryPlan != null) {
                Vector<Integer> retVal = handler.loadProfileMatches(queryPlan);
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

    public EvaluationContext getEvalutionScope() {
        return currentScope;
    }

    public boolean setCurrentEvaluationScopeHint(EvaluationContext context) {
        if(this.currentScope == null ) {
            this.currentScope = context;
            return true;
        } else {
            return false;
        }
    }

    public void unLinkCurrentEvaluationScope() {
        this.currentScope = null;
    }

    public void reportTrace(EvaluationTrace trace) {
        if(currentScope != null) {
            currentScope.reportSubtrace(trace);
        }
    }

    public void setQueryMode(boolean queryModeAggressive) {
        if(this.queryModeAggressive == queryModeAggressive) {
            return;
        }
        this.queryModeAggressive = queryModeAggressive;

        //Clear out state from aggressive queries.
        if(!queryModeAggressive) {
            for(QueryCue cue : aggressiveCues) {
                cue.cleanupCue();
            }
            this.aggressiveCues.clear();
        } else {
            if(queryModeAggressive) {
                for (QueryCue cue : aggressiveCues) {
                    cue.activate();
                }
            }
        }
    }

    public void addQueryCue(QueryCue caseModelFetchCue) {
        this.aggressiveCues.add(caseModelFetchCue);
        if(queryModeAggressive) {
            caseModelFetchCue.activate();
        }
    }

    public <T> T getQueryCue(Class<T> c) {
        for(QueryCue cue : aggressiveCues) {
            if(cue.getClass().equals(c)) {
                return (T)cue;
            }
        }
        return null;
    }
}

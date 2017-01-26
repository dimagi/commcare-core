package org.commcare.cases.query;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;

import java.util.Vector;

import javax.management.Query;

/**
 * A Query Context object is responsible for keeping track of relevant metadata about where a
 * query is executing that may make it possible for the planner to better identify when to
 * trigger certain query handlers.
 *
 * For instance, if an individual query is only looking for one matching case, it may do a single
 * DB read. The context can provide a cue that the query is likely to be run over many other cases,
 * which can provide the planner with a hint to fetch those cases in bulk proactively
 *
 * The QueryContext Object's lifecycle is also used to limit the scope of any of that bulk caching.
 * Since the object lifecycle is paired with the EC of the query, large chunks of memory can be
 * allocated into this object and it will be removed will the context is no longer relevant.
 *
 * Created by ctsims on 1/26/2017.
 */

public class QueryContext {

    public static final int BULK_QUERY_THRESHOLD = 50;

    //TODO: This is a bad reason to keep the EC around here, and locks the lifecycle of this object
    //into the EC
    private EvaluationContext traceRoot;

    private QueryCache cache;

    private QueryContext potentialSpawnedContext;

    //Until we can keep track more robustly of the individual spheres of 'bulk' models
    //we'll just keep track of the dominant factor in our queries to know what to expect
    //WRT whether optimizatons will help or hurt
    int contextScope = 1;

    public QueryContext() {
        cache = new QueryCache();
    }

    public QueryContext(QueryContext parent) {
        this.traceRoot = parent.traceRoot;
        this.cache = new QueryCache(parent.cache);
        this.contextScope = parent.contextScope;
    }

    public QueryContext openNewQueryContext() {
        return new QueryContext(this);
    }

    public QueryContext checkForDerivativeContextAndReturn(int newScope) {
        QueryContext newContext = potentialSpawnedContext;
        potentialSpawnedContext = null;
        if(potentialSpawnedContext == null) { newContext = new QueryContext(this);}
        newContext.contextScope = newScope;

        if(dominates(newContext.contextScope, this.contextScope)) {
            this.reportContextEscalation(this, newContext, "New");
            return newContext;
        } else {
            return this;
        }
    }

    public QueryContext testForInlineScopeEscalation(int newScope) {
        if(dominates(newScope, contextScope)) {
            potentialSpawnedContext = new QueryContext(this);
            potentialSpawnedContext.contextScope = newScope;
            reportContextEscalation(this, potentialSpawnedContext, "Temporary");
            return potentialSpawnedContext;
        } else {
            return this;
        }
    }

    private boolean dominates(int newScope, int existingScope) {
        return newScope > existingScope &&
                newScope > BULK_QUERY_THRESHOLD &&
                newScope / existingScope > 10;
    }

    private void reportContextEscalation(QueryContext queryContext, QueryContext newContext, String label) {
        EvaluationTrace trace = new EvaluationTrace(label + " Query Context [" + newContext.contextScope +"]");
        trace.setOutcome("");
        reportTrace(trace);
    }

    public void reportTrace(EvaluationTrace trace) {
        traceRoot.reportSubtrace(trace);
    }

    public void setTraceRoot(EvaluationContext traceRoot) {
        this.traceRoot = traceRoot;
    }


    public <T extends QueryCacheEntry> T getQueryCache(Class<T> cacheType) {
        return cache.getQueryCache(cacheType);
    }
}

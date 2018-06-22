package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsUncacheableExpressionAnalyzer;
import org.javarosa.xpath.analysis.ReferencesMainInstanceAnalyzer;
import org.javarosa.xpath.analysis.TopLevelContextTypesAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

import java.util.Set;

/**
 * Superclass for an XPathExpression that keeps track of all information related to if it can be
 * cached, and contains wrapper functions for all caching operations.
 *
 * @author Aliza Stone
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    private Object justRetrieved;
    CacheableExprState cacheState = new CacheableExprState();

    boolean isCached(EvaluationContext ec) {
        if (ec.expressionCachingEnabled()) {
            queueUpCachedValue(ec);
            return justRetrieved != null;
        }
        return false;
    }

    private void queueUpCachedValue(EvaluationContext ec) {
        justRetrieved = ec.expressionCacher().getCachedValue(cacheKey(ec));
    }

    /**
     * queueUpCachedValue must always be called first!
     */
    Object getCachedValue() {
        return justRetrieved;
    }

    void cache(Object value, EvaluationContext ec) {
        if (ec.expressionCachingEnabled() &&
                expressionIsCacheable(ec) &&
                relevantContextNodesAreCacheable(ec)) {
            ec.expressionCacher().cache(cacheKey(ec), value);
        }
    }

    private ExpressionCacheKey cacheKey(EvaluationContext ec) {
        return new ExpressionCacheKey(
                this,
                cacheState.contextRefIsRelevant ? ec.getContextRef() : null,
                cacheState.originalContextRefIsRelevant ? ec.getOriginalContext() : null);
    }

    private boolean expressionIsCacheable(EvaluationContext ec) {
        if (!cacheState.computedCacheability) {
            cacheState.exprIsCacheable = rootExpressionTypeIsCacheable() && fullExpressionIsCacheable(ec);
            cacheState.computedCacheability = true;
        }
        return cacheState.exprIsCacheable;
    }

    protected boolean rootExpressionTypeIsCacheable() {
        return true;
    }

    private boolean fullExpressionIsCacheable(EvaluationContext ec) {
        try {
            return !referencesMainFormInstance(this, ec) &&
                    !containsUncacheableSubExpression(this, ec);
        } catch (AnalysisInvalidException e) {
            // If the analysis didn't complete then we assume it's not cacheable
            return false;
        }
    }

    public static boolean referencesMainFormInstance(XPathAnalyzable expr, EvaluationContext ec)
            throws AnalysisInvalidException {
        return (new ReferencesMainInstanceAnalyzer(ec)).computeResult(expr);
    }

    public static boolean containsUncacheableSubExpression(XPathAnalyzable expr, EvaluationContext ec)
            throws AnalysisInvalidException {
        return (new ContainsUncacheableExpressionAnalyzer(ec)).computeResult(expr);
    }

    public boolean relevantContextNodesAreCacheable(EvaluationContext ec) {
        if (!cacheState.computedContextTypes) {
            Set<Integer> relevantContextTypes =
                    new TopLevelContextTypesAnalyzer().accumulate(this);
            cacheState.contextRefIsRelevant =
                    relevantContextTypes.contains(TreeReference.CONTEXT_INHERITED);
            cacheState.originalContextRefIsRelevant =
                    relevantContextTypes.contains(TreeReference.CONTEXT_ORIGINAL);
            cacheState.computedContextTypes = true;
        }
        return !(cacheState.contextRefIsRelevant &&
                contextRefIsUncacheableInForm(ec.getContextRef()))
                &&
                !(cacheState.originalContextRefIsRelevant &&
                        contextRefIsUncacheableInForm(ec.getOriginalContext()));
    }

    /**
     * Why this is true: Since a context ref in an EvaluationContext will always be fully-qualified,
     * its context type will always be either CONTEXT_INSTANCE or CONTEXT_ABSOLUTE. Within a form,
     * CONTEXT_ABSOLUTE always means the context ref is in the main form instance, and is therefore
     * uncacheable, while CONTEXT_INSTANCE always means it is in an external instance, and is
     * therefore cacheable
     */
    private static boolean contextRefIsUncacheableInForm(TreeReference contextRef) {
        return contextRef.getContextType() == TreeReference.CONTEXT_ABSOLUTE;
    }

}

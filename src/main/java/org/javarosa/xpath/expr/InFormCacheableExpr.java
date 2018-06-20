package org.javarosa.xpath.expr;

import org.commcare.util.LogTypes;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
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
    protected boolean computedCacheability;
    protected boolean exprIsCacheable;
    protected boolean computedContextTypes;
    protected boolean contextRefIsRelevant;
    protected boolean originalContextRefIsRelevant;

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
                contextRefIsRelevant ? ec.getContextRef() : null,
                originalContextRefIsRelevant ? ec.getOriginalContext() : null);
    }

    protected boolean expressionIsCacheable(EvaluationContext ec) {
        if (!computedCacheability) {
            exprIsCacheable = rootExpressionTypeIsCacheable() && fullExpressionIsCacheable(ec);
            computedCacheability = true;
        }
        return exprIsCacheable;
    }

    protected boolean rootExpressionTypeIsCacheable() {
        return true;
    }

    private boolean fullExpressionIsCacheable(EvaluationContext ec) {
        if (ec.getMainInstance() instanceof FormInstance) {
            try {
                return !referencesMainFormInstance(this, (FormInstance)ec.getMainInstance(), ec) &&
                        !containsUncacheableSubExpression(this, ec);
            } catch (AnalysisInvalidException e) {
                // If the analysis didn't complete then we assume it's not cacheable
                return false;
            }
        } else {
            Logger.log(LogTypes.SOFT_ASSERT,
                    "Caching was enabled in the ec, but the main instance provided " +
                            "to InFormCacheableExpr by the ec was not of type FormInstance: " + ec.getMainInstance());
            return false;
        }
    }

    public static boolean referencesMainFormInstance(XPathAnalyzable expr, FormInstance formInstance, EvaluationContext ec) throws AnalysisInvalidException {
        String formInstanceRoot = formInstance.getBase().getChildAt(0).getName();
        return (new ReferencesMainInstanceAnalyzer(formInstanceRoot, ec)).computeResult(expr);
    }

    public static boolean containsUncacheableSubExpression(XPathAnalyzable expr, EvaluationContext ec) throws AnalysisInvalidException {
        return (new ContainsUncacheableExpressionAnalyzer(ec)).computeResult(expr);
    }

    private boolean relevantContextNodesAreCacheable(EvaluationContext ec) {
        if (!computedContextTypes) {
            Set<Integer> relevantContextTypes = new TopLevelContextTypesAnalyzer().accumulate(this);
            contextRefIsRelevant = relevantContextTypes.contains(TreeReference.CONTEXT_INHERITED);
            originalContextRefIsRelevant = relevantContextTypes.contains(TreeReference.CONTEXT_ORIGINAL);
            computedContextTypes = true;
        }
        return !(contextRefIsRelevant && contextRefIsUncacheable(ec.getContextRef()))
                &&
                !(originalContextRefIsRelevant && contextRefIsUncacheable(ec.getOriginalContext()));
    }

    /**
     * Why this is true: Since a context ref in an EvaluationContext will always be fully-qualified,
     * its context type will always be either CONTEXT_INSTANCE or CONTEXT_ABSOLUTE. Within a form,
     * CONTEXT_ABSOLUTE always means the context ref is in the main form instance, and is therefore
     * uncacheable, while CONTEXT_INSTANCE always means it is in an external instance, and is
     * therefore cacheable
     */
    private static boolean contextRefIsUncacheable(TreeReference contextRef) {
        return contextRef.getContextType() == TreeReference.CONTEXT_ABSOLUTE;
    }

}

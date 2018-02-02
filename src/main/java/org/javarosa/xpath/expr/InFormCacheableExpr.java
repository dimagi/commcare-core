package org.javarosa.xpath.expr;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.InFormExpressionCacher;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsUncacheableExpressionAnalyzer;
import org.javarosa.xpath.analysis.ReferencesMainInstanceAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

/**
 * Superclass for an XPathExpression that keeps track of all information related to if it can be
 * cached, and contains wrapper functions for all caching operations.
 *
 * @author Aliza Stone
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    private Object justRetrieved;
    protected boolean computedCacheability;
    protected boolean isCacheable;

    private static InFormExpressionCacher cacher = new InFormExpressionCacher();

    protected boolean isCached() {
        queueUpCachedValue();
        return justRetrieved != null;
    }

    private void queueUpCachedValue() {
        if (environmentValidForCaching()) {
            justRetrieved = cacher.getCachedValue(this);
        } else {
            justRetrieved = null;
        }
    }

    Object getCachedValue() {
        return justRetrieved;
    }

     void cache(Object value) {
        if (isCacheable()) {
            cacher.cache(this, value);
        }
    }

    protected boolean isCacheable() {
        if (!computedCacheability) {
            computeCacheability();
        }
        return isCacheable;
    }

    private void computeCacheability() {
        if (environmentValidForCaching()) {
            try {
                isCacheable = !referencesMainFormInstance() && !containsUncacheableSubExpression();
            } catch (AnalysisInvalidException e) {
                // if the analysis didn't complete then we assume it's not cacheable
                isCacheable = false;
            }
            computedCacheability = true;
        }
    }

    private boolean referencesMainFormInstance() throws AnalysisInvalidException {
        return (new ReferencesMainInstanceAnalyzer(cacher.getFormInstanceRoot()))
                .computeResult(this);
    }

    private boolean containsUncacheableSubExpression() throws AnalysisInvalidException {
        return (new ContainsUncacheableExpressionAnalyzer()).computeResult(this);
    }

    private boolean environmentValidForCaching() {
        return cacher.getFormInstanceRoot() != null;
    }

    public static void enableCaching(FormInstance formInstance, boolean clearCacheFirst) {
        if (clearCacheFirst) {
            cacher.wipeCache();
        }
        cacher.setFormInstanceRoot(formInstance.getBase().getChildAt(0).getName());
    }

    public static void disableCaching() {
        cacher.setFormInstanceRoot(null);
    }

}

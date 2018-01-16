package org.javarosa.xpath.expr;

import org.javarosa.core.services.InFormExpressionCacher;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsUncacheableExpressionAnalyzer;
import org.javarosa.xpath.analysis.ReferencesMainInstanceAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    protected int recordIdOfCachedExpression = -1;
    private Object justRetrieved;

    protected boolean isCached() {
        queueUpCachedValue();
        return justRetrieved != null;
    }

    private void queueUpCachedValue() {
        if (environmentValidForCaching() && recordIdOfCachedExpression != -1) {
            //justRetrieved = getExpressionCacher().getCachedValue(recordIdOfCachedExpression);
            justRetrieved = getExpressionCacher().getCachedValue(this);
        } else {
            justRetrieved = null;
        }
    }

    protected Object getCachedValue() {
        return justRetrieved;
    }

    protected void cache(Object value) {
        if (expressionIsCacheable(value)) {
            this.recordIdOfCachedExpression = getExpressionCacher().cache(this, value);
        }
    }

    protected boolean expressionIsCacheable(Object result) {
        if (environmentValidForCaching() && !(result instanceof XPathNodeset)) {
            try {
                return !referencesMainFormInstance() && !containsUncacheableSubExpression();
            } catch (AnalysisInvalidException e) {
                // if the analysis didn't complete then we assume it's not cacheable
            }
        }
        return false;
    }

    private boolean referencesMainFormInstance() throws AnalysisInvalidException {
        return (new ReferencesMainInstanceAnalyzer(getExpressionCacher().formInstanceRoot))
                .computeResult(this);
    }

    private boolean containsUncacheableSubExpression() throws AnalysisInvalidException {
        return (new ContainsUncacheableExpressionAnalyzer()).computeResult(this);
    }

    private boolean environmentValidForCaching() {
        return getExpressionCacher().environmentValidForCaching();
    }

    private InFormExpressionCacher getExpressionCacher() {
        return InFormExpressionCacher.getCacher();
    }

}
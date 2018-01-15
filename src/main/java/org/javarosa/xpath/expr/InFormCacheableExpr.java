package org.javarosa.xpath.expr;

import org.javarosa.core.services.ExpressionCacher;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsMainInstanceRefAnalyzer;
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
            justRetrieved = getExpressionCacher().getCachedValue(recordIdOfCachedExpression);
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

    private boolean expressionIsCacheable(Object result) {
        if (environmentValidForCaching() && !(result instanceof XPathNodeset)) {
            try {
                return !referencesMainFormInstance();
            } catch (AnalysisInvalidException e) {
                // if the analysis didn't complete then we assume it's not cacheable
            }
        }
        return false;
    }

    private boolean referencesMainFormInstance() throws AnalysisInvalidException {
        return (new ContainsMainInstanceRefAnalyzer()).computeResult(this);
    }

    private boolean environmentValidForCaching() {
        return getExpressionCacher().environmentValidForCaching();
    }

    private ExpressionCacher getExpressionCacher() {
        return ExpressionCacher.getCacher();
    }

}

package org.javarosa.xpath;

import org.javarosa.core.services.storage.ExpressionCacher;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsMainInstanceRefAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    protected int recordIdOfCachedExpression = -1;

    protected boolean isCached() {
        return getCachedValue() != null;
    }

    protected Object getCachedValue() {
        if (environmentValidForCaching() && recordIdOfCachedExpression != -1) {
            return getExpressionCacher().getCachedValue(recordIdOfCachedExpression);
        } else {
            return null;
        }
    }

    protected void cache(Object value) {
        if (expressionIsCacheable(value)) {
            this.recordIdOfCachedExpression = getExpressionCacher().cache(this, value);
        }
    }

    private boolean expressionIsCacheable(Object result) {
        if (environmentValidForCaching() && !(result instanceof XPathNodeset)) {
            try {
                return (new ContainsMainInstanceRefAnalyzer()).computeResult(this);
            } catch (AnalysisInvalidException e) {
                // if the analysis didn't complete then we assume it's not cacheable
            }
        }
        return false;
    }

    private boolean environmentValidForCaching() {
        //TODO: figure out how to set this properly
        return false;
    }

    private ExpressionCacher getExpressionCacher() {
        return ExpressionCacher.getCacher();
    }

}

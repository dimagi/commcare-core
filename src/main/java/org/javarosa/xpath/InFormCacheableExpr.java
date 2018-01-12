package org.javarosa.xpath;

import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.StorageProvider;
import org.javarosa.core.services.storage.ExpressionCacheStorage;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsMainInstanceRefAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    protected int recordIdOfCachedExpression;

    protected boolean isCached() {
        return getCachedValue() != null;
    }

    protected Object getCachedValue() {
        if (environmentValidForCaching()) {
            //return getCacheStorage().read(recordIdOfCachedExpression).getEvalResult();
            return getCacheStorage().getCachedValue(this);
        } else {
            return null;
        }
    }

    protected void cache(Object value) {
        if (expressionIsCacheable(value)) {
            CachedExpression ce = new CachedExpression(this, value);
            getCacheStorage().cache(ce);
            this.recordIdOfCachedExpression = ce.getID();
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
        return false;
    }

    private ExpressionCacheStorage getCacheStorage() {
        return StorageProvider.instance().getExpressionCacheStorage();
    }

}

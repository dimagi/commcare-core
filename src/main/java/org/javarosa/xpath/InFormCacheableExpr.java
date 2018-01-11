package org.javarosa.xpath;

import org.javarosa.core.services.storage.ExpressionCacheStorage;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsMainInstanceRefAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    private ExpressionCacheStorage cacheStorage;

    protected boolean isCached() {
        return getCachedValue() != null;
    }

    protected Object getCachedValue() {
        if (environmentValidForCaching()) {
            return cacheStorage.getCachedValue(this);
        } else {
            return null;
        }
    }

    protected boolean isCacheable() {
        if (environmentValidForCaching()) {
            try {
                return (new ContainsMainInstanceRefAnalyzer()).computeResult(this);
            } catch (AnalysisInvalidException e) {
            }
        }
        return false;
    }

    protected void cache(Object value) {
        if (environmentValidForCaching()) {
            cacheStorage.cache(this, value);
        }
    }

    private boolean environmentValidForCaching() {
        return false;
    }

}

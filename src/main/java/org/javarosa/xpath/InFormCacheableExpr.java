package org.javarosa.xpath;

import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsMainInstanceRefAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

import java.util.Map;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    protected boolean isCached() {
        return getCachedValue() != null;
    }

    protected Object getCachedValue() {
        return getCache() == null ? null : getCache().get(this);
    }

    protected boolean isCacheable() {
        if (getCache() == null) {
            return false;
        }

        try {
            return (new ContainsMainInstanceRefAnalyzer()).computeResult(this);
        } catch (AnalysisInvalidException e) {
            return false;
        }
    }

    protected void cache(Object value) {
        if (getCache() != null) {
            getCache().put(this, value);
        }
    }

    private Map<InFormCacheableExpr, Object> getCache() {
        return null;
    }

}

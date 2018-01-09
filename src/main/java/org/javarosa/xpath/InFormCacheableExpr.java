package org.javarosa.xpath;

import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsMainInstanceRefAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

import java.util.Map;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    public boolean isCached() {
        return getCachedValue() != null;
    }

    public Object getCachedValue() {
        return getCache() == null ? null : getCache().get(this);
    }

    public boolean isCacheable() {
        if (getCache() == null) {
            return false;
        }

        try {
            return (new ContainsMainInstanceRefAnalyzer()).computeResult(this);
        } catch (AnalysisInvalidException e) {
            return false;
        }
    }

    public void cache(Object value) {
        if (getCache() != null) {
            getCache().put(this, value);
        }
    }

    private Map<InFormCacheableExpr, Object> getCache() {
        return null;
    }

}

package org.javarosa.core.services;

import org.javarosa.xpath.expr.InFormCacheableExpr;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Aliza Stone
 */
public class InFormExpressionCacher {

    private Map<InFormCacheableExpr, Object> cache;

    public InFormExpressionCacher() {
        cache = new HashMap<>();
    }

    public void cache(InFormCacheableExpr expression, Object value) {
        cache.put(expression, value);
    }

    public Object getCachedValue(InFormCacheableExpr expression) {
        return cache.get(expression);
    }

    public void clearCache() {
        cache.clear();
    }

    public boolean hasCachedValues() {
        return !cache.isEmpty();
    }

}

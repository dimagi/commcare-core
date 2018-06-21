package org.javarosa.xpath.expr;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Aliza Stone
 */
public class ExpressionCacher {

    private Map<ExpressionCacheKey, Object> cache;

    public ExpressionCacher() {
        cache = new HashMap<>();
    }

    public void cache(ExpressionCacheKey cacheKey, Object value) {
        cache.put(cacheKey, value);
    }

    public Object getCachedValue(ExpressionCacheKey cacheKey) {
        return cache.get(cacheKey);
    }

}

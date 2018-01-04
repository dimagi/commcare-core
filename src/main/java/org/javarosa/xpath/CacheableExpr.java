package org.javarosa.xpath;

import java.util.Map;

/**
 * Created by amstone326 on 1/4/18.
 */

public abstract class CacheableExpr {

    public boolean isCached() {
        return getCachedValue() != null;
    }

    public Object getCachedValue() {
        return getCache().get(this);
    }

    public abstract boolean isCacheable();

    public void cache(Object value) {
        getCache().put(this, value);
    }

    private Map<CacheableExpr, Object> getCache() {
        return null;
    }

}

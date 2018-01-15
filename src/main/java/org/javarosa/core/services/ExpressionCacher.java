package org.javarosa.core.services;

import org.javarosa.xpath.expr.InFormCacheableExpr;

/**
 * Created by amstone326 on 1/10/18.
 */

public class ExpressionCacher {

    // dummy implementations

    public int cache(InFormCacheableExpr expression, Object value) {
        return -1;
    }

    public Object getCachedValue(int idOfStoredCache) {
        return null;
    }

    public boolean environmentValidForCaching() {
        return false;
    }

    public void wipeCacheStorage() {

    }

    //

    private static ExpressionCacher cacher = new ExpressionCacher();

    public static void setCacher(ExpressionCacher cacherForEnvironment) {
        cacher = cacherForEnvironment;
    }

    public static ExpressionCacher getCacher() {
        return cacher;
    }

    public static void reset() {
        cacher.wipeCacheStorage();
        cacher = new ExpressionCacher();
    }

}

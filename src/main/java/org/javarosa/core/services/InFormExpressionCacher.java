package org.javarosa.core.services;

import org.javarosa.xpath.expr.InFormCacheableExpr;

/**
 * Created by amstone326 on 1/10/18.
 */

public class InFormExpressionCacher {

    public String formInstanceRoot;

    public InFormExpressionCacher() {
    }

    // dummy implementations

    public int cache(InFormCacheableExpr expression, Object value) {
        return -1;
    }

    public Object getCachedValue(int idOfStoredCache) {
        return null;
    }

    public Object getCachedValue(InFormCacheableExpr expression) {
        return null;
    }

    public boolean environmentValidForCaching() {
        return false;
    }

    public void wipeCache() {

    }

    //

    private static InFormExpressionCacher cacher = new InFormExpressionCacher();

    public static void setCacher(InFormExpressionCacher cacherForEnvironment) {
        cacher = cacherForEnvironment;
    }

    public static InFormExpressionCacher getCacher() {
        return cacher;
    }

    public static void reset() {
        cacher.wipeCache();
        cacher = new InFormExpressionCacher();
    }

}

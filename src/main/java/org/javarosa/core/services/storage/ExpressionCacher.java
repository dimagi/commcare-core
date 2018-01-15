package org.javarosa.core.services.storage;

import org.javarosa.xpath.InFormCacheableExpr;

/**
 * Created by amstone326 on 1/10/18.
 */

public abstract class ExpressionCacher {

    public abstract int cache(InFormCacheableExpr expression, Object value);
    public abstract Object getCachedValue(int idOfStoredCache);

    static ExpressionCacher cacher = new DummyExpressionCacher();

    public static void setCacher(ExpressionCacher cacherForEnvironment) {
        cacher = cacherForEnvironment;
    }

    public static ExpressionCacher getCacher() {
        return cacher;
    }

}

package org.javarosa.core.services.storage;

import org.javarosa.xpath.InFormCacheableExpr;

/**
 * Created by amstone326 on 1/11/18.
 */

public class DummyExpressionCacher extends ExpressionCacher {

    @Override
    public int cache(InFormCacheableExpr expression, Object value) {
        return -1;
    }

    @Override
    public Object getCachedValue(int idOfStoredCache) {
        return null;
    }
}

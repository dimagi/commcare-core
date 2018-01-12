package org.javarosa.core.services.storage;

import org.javarosa.xpath.CachedExpression;
import org.javarosa.xpath.InFormCacheableExpr;

/**
 * Created by amstone326 on 1/11/18.
 */

public class DummyExpressionCacheStorage implements ExpressionCacheStorage {

    @Override
    public void cache(CachedExpression value) {

    }

    @Override
    public Object getCachedValue(InFormCacheableExpr key) {
        return null;
    }
}

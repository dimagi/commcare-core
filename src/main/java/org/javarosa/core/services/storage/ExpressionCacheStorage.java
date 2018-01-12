package org.javarosa.core.services.storage;

import org.javarosa.xpath.CachedExpression;
import org.javarosa.xpath.InFormCacheableExpr;

/**
 * Created by amstone326 on 1/10/18.
 */

public interface ExpressionCacheStorage {

    void cache(CachedExpression value);
    Object getCachedValue(InFormCacheableExpr key);

}

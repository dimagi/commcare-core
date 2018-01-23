package org.javarosa.core.services;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.xpath.expr.InFormCacheableExpr;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by amstone326 on 1/10/18.
 */

public class InFormExpressionCacher {

    private Map<InFormCacheableExpr, Object> cache;
    private Map<InFormCacheableExpr, Integer> cacheRetrievalCounts;
    protected String formInstanceRoot;

    public InFormExpressionCacher() {
        cache = new HashMap<>();
    }

    public void cache(InFormCacheableExpr expression, Object value) {
        cache.put(expression, value);
    }

    public Object getCachedValue(InFormCacheableExpr expression) {
        //cacheRetrievalCounts.put(expression, cacheRetrievalCounts.get(expression) + 1);
        return cache.get(expression);
    }

    public void wipeCache() {
        cache.clear();
    }

    public void setFormInstanceRoot(FormInstance formInstance) {
        this.formInstanceRoot = formInstance.getBase().getChildAt(0).getName();
    }

    public String getFormInstanceRoot() {
        return this.formInstanceRoot;
    }

}

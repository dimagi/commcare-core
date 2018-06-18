package org.javarosa.xpath.expr;

import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by amstone326 on 3/23/18.
 */

public class ExpressionCacheKey {

    private InFormCacheableExpr expr;

    public ExpressionCacheKey(InFormCacheableExpr expr) {
        this.expr = expr;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ExpressionCacheKey) {
            ExpressionCacheKey other = (ExpressionCacheKey)o;
            return expr.equals(other.expr);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return expr.hashCode();
    }

}

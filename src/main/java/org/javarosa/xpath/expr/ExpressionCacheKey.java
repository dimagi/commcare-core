package org.javarosa.xpath.expr;

import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by amstone326 on 3/23/18.
 */

public class ExpressionCacheKey {

    private InFormCacheableExpr expr;
    private TreeReference context;

    public ExpressionCacheKey(InFormCacheableExpr expr, TreeReference contextNode) {
        this.expr = expr;
        this.context = contextNode;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ExpressionCacheKey) {
            ExpressionCacheKey other = (ExpressionCacheKey)o;
            return expr.equals(other.expr) && context.equals(other.context);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return expr.hashCode() ^ context.hashCode();
    }

}

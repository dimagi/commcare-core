package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * Created by amstone326 on 3/23/18.
 */

public class ExpressionCacheKey {

    private InFormCacheableExpr expr;

    // These are each only relevant to caching in some cases, and will be null in a cache key for
    // which they are not relevant
    @Nullable
    private TreeReference contextRef;
    @Nullable
    private TreeReference originalContextRef;

    ExpressionCacheKey(InFormCacheableExpr expr, TreeReference contextRef, TreeReference originalContextRef) {
        this.expr = expr;
        this.contextRef = contextRef;
        this.originalContextRef = originalContextRef;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ExpressionCacheKey) {
            ExpressionCacheKey other = (ExpressionCacheKey)o;
            return this.expr.equals(other.expr) && contextsEqual(this, other) && originalContextsEqual(this, other);
        }
        return false;
    }


    private static boolean contextsEqual(ExpressionCacheKey ck1, ExpressionCacheKey ck2) {
        if (ck1.contextRef == null) {
            return ck2.contextRef == null;
        } else {
            return ck1.contextRef.equals(ck2.contextRef);
        }
    }

    private static boolean originalContextsEqual(ExpressionCacheKey ck1, ExpressionCacheKey ck2) {
        if (ck1.originalContextRef == null) {
            return ck2.originalContextRef == null;
        } else {
            return ck1.originalContextRef.equals(ck2.originalContextRef);
        }
    }

    @Override
    public int hashCode() {
        int hash = expr.hashCode();
        if (contextRef != null) {
            hash ^= contextRef.hashCode();
        }
        if (originalContextRef != null) {
            hash ^= originalContextRef.hashCode();
        }
        return hash;
    }

}

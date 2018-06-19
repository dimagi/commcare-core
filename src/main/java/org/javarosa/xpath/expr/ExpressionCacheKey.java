package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.analysis.TopLevelContextTypesAnalyzer;

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

    public ExpressionCacheKey(InFormCacheableExpr expr, EvaluationContext ec) {
        this.expr = expr;
        for (int contextType : getRelevantContextTypes(expr)) {
            if (contextType == TreeReference.CONTEXT_INHERITED) {
                contextRef = ec.getContextRef();
            } else if (contextType == TreeReference.CONTEXT_ORIGINAL) {
                originalContextRef = ec.getOriginalContext();
            }
        }
    }

    private static Set<Integer> getRelevantContextTypes(InFormCacheableExpr expr) {
        return new TopLevelContextTypesAnalyzer().accumulate(expr);
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

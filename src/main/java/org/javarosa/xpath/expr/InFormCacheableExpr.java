package org.javarosa.xpath.expr;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.InFormExpressionCacher;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsUncacheableExpressionAnalyzer;
import org.javarosa.xpath.analysis.ReferencesMainInstanceAnalyzer;
import org.javarosa.xpath.analysis.XPathAnalyzable;

/**
 * Created by amstone326 on 1/4/18.
 */
public abstract class InFormCacheableExpr implements XPathAnalyzable {

    private Object justRetrieved;

    protected boolean isCached() {
        queueUpCachedValue();
        return justRetrieved != null;
    }

    private void queueUpCachedValue() {
        if (environmentValidForCaching()) {
            justRetrieved = cacher.getCachedValue(this);
        } else {
            justRetrieved = null;
        }
    }

    Object getCachedValue() {
        System.out.println("Returning cached value for expression: " + ((XPathExpression)this).toPrettyString());
        return justRetrieved;
    }

     void cache(Object value) {
        if (expressionIsCacheable(value)) {
            cacher.cache(this, value);
        }
    }

    protected boolean expressionIsCacheable(Object result) {
        if (environmentValidForCaching()) {
            try {
                boolean b = !referencesMainFormInstance() && !containsUncacheableSubExpression();
                System.out.println("is cacheable: " + b + " " + ((XPathExpression)this).toPrettyString());
                return b;
            } catch (AnalysisInvalidException e) {
                // if the analysis didn't complete then we assume it's not cacheable
            }
        }
        return false;
    }

    private boolean referencesMainFormInstance() throws AnalysisInvalidException {
        return (new ReferencesMainInstanceAnalyzer(cacher.getFormInstanceRoot()))
                .computeResult(this);
    }

    private boolean containsUncacheableSubExpression() throws AnalysisInvalidException {
        return (new ContainsUncacheableExpressionAnalyzer()).computeResult(this);
    }

    private boolean environmentValidForCaching() {
        return cacher.getFormInstanceRoot() != null;
    }

    private static InFormExpressionCacher cacher = new InFormExpressionCacher();

    public static void enableCaching(FormInstance formInstance, boolean clearCacheFirst) {
        if (clearCacheFirst) {
            cacher.wipeCache();
        }
        cacher.setFormInstanceRoot(formInstance.getBase().getChildAt(0).getName());
    }

    public static void disableCaching() {
        cacher.setFormInstanceRoot(null);
    }

}

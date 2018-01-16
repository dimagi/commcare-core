package org.javarosa.xpath.analysis;

import org.javarosa.xpath.expr.UncacheableXPathFuncExpr;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * Created by amstone326 on 1/16/18.
 */

public class ContainsUncacheableExpressionAnalyzer extends XPathBooleanAnalyzer {

    @Override
    public void doAnalysis(XPathFuncExpr expr) {
        if (expr instanceof UncacheableXPathFuncExpr) {
            this.result = true;
        }
    }

    @Override
    protected boolean getDefaultValue() {
        return false;
    }

    @Override
    protected boolean aggregateResults() {
        return orResults();
    }

    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new ContainsUncacheableExpressionAnalyzer();
    }
}

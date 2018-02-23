package org.javarosa.xpath.analysis;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.UncacheableXPathFuncExpr;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * Analyzes an XPath expression to determine whether it is or contains an XPathFuncExpr that is
 * un-cacheable by its nature (such as now() or random()).
 *
 * @author Aliza Stone
 */
public class ContainsUncacheableExpressionAnalyzer extends XPathBooleanAnalyzer {

    public ContainsUncacheableExpressionAnalyzer(EvaluationContext ec) {
        super();
        setContext(ec);
    }

    public ContainsUncacheableExpressionAnalyzer() {
        super();
    }

    @Override
    public void doAnalysis(XPathFuncExpr expr) {
        if (expr instanceof UncacheableXPathFuncExpr) {
            this.result = true;
            this.shortCircuit = true;
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

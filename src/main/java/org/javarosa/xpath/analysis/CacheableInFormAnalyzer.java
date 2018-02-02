package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.UncacheableXPathFuncExpr;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * Analyzes an XPath expression to determine if it is safely cacheable within a form context. There
 * are 2 checks that need to be done to know this:
 *
 * 1) The expression must not contain a reference to the main data instance of the form. This check
 * is performed by the implementation of doNormalTreeRefAnalysis() below.
 * 2) The expression must not be or contain an expression that is by its nature un-cacheable. This
 * check is performed by the implementation of doAnalysis() for an XPathFuncExpr below.
 *
 * @author Aliza Stone
 */
public class CacheableInFormAnalyzer extends XPathBooleanAnalyzer {

    private final String mainInstanceRoot;

    public CacheableInFormAnalyzer(String instanceName) {
        mainInstanceRoot = instanceName;
    }

    @Override
    public void doAnalysis(XPathFuncExpr expr) {
        if (expr instanceof UncacheableXPathFuncExpr) {
            this.result = false;
            this.shortCircuit = true;
        }
    }

    @Override
    public void doNormalTreeRefAnalysis(TreeReference treeRef) throws AnalysisInvalidException {
        if (treeRef.getName(0).equals(mainInstanceRoot)) {
            this.result = false;
            this.shortCircuit = true;
        }
    }

    @Override
    protected boolean getDefaultValue() {
        return true;
    }

    @Override
    protected boolean aggregateResults() {
        return orResults();
    }

    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new CacheableInFormAnalyzer(mainInstanceRoot);
    }
}

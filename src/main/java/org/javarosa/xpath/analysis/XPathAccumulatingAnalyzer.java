package org.javarosa.xpath.analysis;

import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.List;

/**
 * Created by amstone326 on 8/11/17.
 */

public abstract class XPathAccumulatingAnalyzer<T> {

    protected List<T> accumulatedList;

    public void extractTargetValues(XPathAnalyzable analyzable) {
    }

    public void extractTargetValues(XPathPathExpr analyzable) {
    }

    public List<T> accumulate(XPathAnalyzable rootExpression) {
        rootExpression.applyAndPropagateAccumulatingAnalyzer(this);
        return accumulatedList;
    }

}

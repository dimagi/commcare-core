package org.javarosa.xpath.analysis;

import org.javarosa.xpath.expr.XPathExpression;

/**
 * Created by amstone326 on 8/9/17.
 */

public abstract class XPathBooleanAnalyzer {

    private XPathExpression expression;

    public XPathBooleanAnalyzer(XPathExpression expr) {
        this.expression = expr;
    }

    public void doAnalysis() {

    }

    abstract boolean conditionToCheckOnExpr();

}

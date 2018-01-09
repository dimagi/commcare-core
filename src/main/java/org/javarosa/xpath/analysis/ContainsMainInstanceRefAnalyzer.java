package org.javarosa.xpath.analysis;

import org.javarosa.xpath.expr.XPathStep;

/**
 * Created by amstone326 on 1/4/18.
 */

public class ContainsMainInstanceRefAnalyzer extends XPathBooleanAnalyzer {

    @Override
    public void doAnalysis(XPathStep step) throws AnalysisInvalidException {
        if (step.name.name.equals("data")) {
            this.result = true;
            this.shortCircuit = true;
        }
    }

    @Override
    protected boolean getDefaultValue() {
        return false;
    }

    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new ContainsMainInstanceRefAnalyzer();
    }
}

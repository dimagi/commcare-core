package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;
/**
 * Analyzes an XPath expression to determine whether it contains a reference to the main data
 * instance
 *
 * @author Aliza Stone
 */
public class ReferencesMainInstanceAnalyzer extends XPathBooleanAnalyzer {

    private final String mainInstanceRoot;

    public ReferencesMainInstanceAnalyzer(String instanceName) {
        this.mainInstanceRoot = instanceName;
    }

    @Override
    public void doNormalTreeRefAnalysis(TreeReference treeRef) throws AnalysisInvalidException {
        if (treeRef.getName(0).equals(mainInstanceRoot)) {
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

    XPathAnalyzer initSameTypeAnalyzer() {
        return new ReferencesMainInstanceAnalyzer(mainInstanceRoot);
    }
}

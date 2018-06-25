package org.javarosa.xpath.analysis;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Analyzes an XPath expression to determine whether it contains a reference to the main data
 * instance.
 *
 * @author Aliza Stone
 */
public class ReferencesMainInstanceAnalyzer extends XPathBooleanAnalyzer {

    public ReferencesMainInstanceAnalyzer(EvaluationContext ec) {
        this();
        setContext(ec);
    }

    public ReferencesMainInstanceAnalyzer() {
        super();
    }

    @Override
    public void doNormalTreeRefAnalysis(TreeReference treeRef) throws AnalysisInvalidException {
        if (referenceRefersToMainInstance(treeRef)) {
            this.result = true;
            this.shortCircuit = true;
        }
    }

    private boolean referenceRefersToMainInstance(TreeReference treeRef) {
        return treeRef.getContextType() == TreeReference.CONTEXT_ABSOLUTE &&
                treeRef.getInstanceName() == null;
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
        return new ReferencesMainInstanceAnalyzer();
    }
}

package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

/**
 * An XPathAccumulatingAnalyzer that collects all of the TreeReference context types that are relevant
 * to the evaluation of JUST the top level of the given expression, i.e. not including predicates
 *
 * @author Aliza Stone
 */
public class TopLevelContextTypesAnalyzer extends XPathAccumulatingAnalyzer<Integer> {

    @Override
    public void doAnalysis(TreeReference ref) throws AnalysisInvalidException {
        addToResult(ref.getContextType());
    }

    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new TopLevelContextTypesAnalyzer();
    }

    @Override
    public boolean shouldIncludePredicates() {
        return false;
    }

    @Override
    public boolean shortCircuit() {
        // If we've gotten them all then no need to keep going
        return size() == TreeReference.CONTEXT_TYPES.length;
    }

}

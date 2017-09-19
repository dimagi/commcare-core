package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;

/**
 * An XPathAccumulatingAnalyzer that collects all of the instance names that are referenced
 * in an expression
 *
 * @author Aliza Stone
 */

public class InstanceNameAccumulatingAnalyzer extends XPathAccumulatingAnalyzer<String> {

    public InstanceNameAccumulatingAnalyzer() {
        super();
    }

    public InstanceNameAccumulatingAnalyzer(TreeReference contextRef) {
        super();
        setContext(contextRef);
    }

    @Override
    public void doNormalTreeRefAnalysis(TreeReference treeRef) throws AnalysisInvalidException {
        if (treeRef.getContextType() == TreeReference.CONTEXT_INSTANCE) {
            addResultToList(treeRef.getInstanceName());
        }
    }

    // For all AccumulatingAnalyzers, it should be sufficient to handle a current() reference by
    // applying the analyzer separately to the expression itself and to the context ref
    @Override
    public void doAnalysisForTreeRefWithCurrent(TreeReference expressionWithContextTypeCurrent)
            throws AnalysisInvalidException {

        requireOriginalContext(expressionWithContextTypeCurrent);
        doNormalTreeRefAnalysis(expressionWithContextTypeCurrent);

        // TODO: There may be a way to figure out more carefully under which circumstances
        // getOriginalContextRef() actually represents something new to analyze (similar to the
        // check below in doAnalysisForRelativeTreeRef), but for now this seems hard to determine
        getOriginalContextRef().applyAndPropagateAnalyzer(this);
    }

    // For all AccumulatingAnalyzers, it should be sufficient to handle a relative reference by
    // applying the analyzer separately to the expression itself and to the context ref
    @Override
    public void doAnalysisForRelativeTreeRef(TreeReference expressionWithContextTypeRelative)
            throws AnalysisInvalidException {

        requireContext(expressionWithContextTypeRelative);
        doNormalTreeRefAnalysis(expressionWithContextTypeRelative);

        if (!this.isSubAnalyzer) {
            // Relative refs only introduce something new to analyze if they are in the top-level
            // expression
            getContextRef().applyAndPropagateAnalyzer(this);
        }
    }


    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new InstanceNameAccumulatingAnalyzer();
    }

}

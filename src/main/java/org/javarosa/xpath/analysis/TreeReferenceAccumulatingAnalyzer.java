package org.javarosa.xpath.analysis;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;

/**
 *
 * Accumulates all of the treereferences that are included in a given xpath expression.
 *
 *
 * TODO: Once we use the fork/join pattern, this analyzer should possibly detect in "no-context"
 * mode that current() was dereferenced to mean "./", which is a huge potential source of issues.
 *
 * Created by ctsims on 9/15/2017.
 */

public class TreeReferenceAccumulatingAnalyzer extends XPathAccumulatingAnalyzer<TreeReference>  {

    public TreeReferenceAccumulatingAnalyzer() {
        super();
    }
    public TreeReferenceAccumulatingAnalyzer(EvaluationContext context) {
        super();
        setContext(context);
    }

    @Override
    public void doNormalTreeRefAnalysis(TreeReference treeRef) throws AnalysisInvalidException {
        addResultToList(treeRef.removePredicates());
    }

    // For all AccumulatingAnalyzers, it should be sufficient to handle a current() reference by
    // applying the analyzer separately to the expression itself and to the context ref
    @Override
    public void doAnalysisForTreeRefWithCurrent(TreeReference expressionWithContextTypeCurrent)
            throws AnalysisInvalidException {

        requireOriginalContext(expressionWithContextTypeCurrent);
        doNormalTreeRefAnalysis(expressionWithContextTypeCurrent.contextualize(getOriginalContextRef()));
    }

    // For all AccumulatingAnalyzers, it should be sufficient to handle a relative reference by
    // applying the analyzer separately to the expression itself and to the context ref
    @Override
    public void doAnalysisForRelativeTreeRef(TreeReference expressionWithContextTypeRelative)
            throws AnalysisInvalidException {

        requireContext(expressionWithContextTypeRelative);

        doNormalTreeRefAnalysis(expressionWithContextTypeRelative.contextualize(this.getContextRef()));
    }


    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new TreeReferenceAccumulatingAnalyzer();
    }

}

package org.javarosa.xpath.analysis;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;

/**
 * Accumulates all of the treereferences that are included in a given xpath expression.
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

    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new TreeReferenceAccumulatingAnalyzer();
    }

}

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

    @Override
    public void doAnalysisForRelativeTreeRef(TreeReference expressionWithContextTypeRelative)
            throws AnalysisInvalidException {
        if (!this.isSubAnalyzer) {
            // For instance accumulation, relative refs only introduce something new to analyze
            // if they are in the top-level expression
            super.doAnalysisForRelativeTreeRef(expressionWithContextTypeRelative);
        } else {
            doNormalTreeRefAnalysis(expressionWithContextTypeRelative);
        }
    }


    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new InstanceNameAccumulatingAnalyzer();
    }

}

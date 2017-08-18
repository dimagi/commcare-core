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
        this.accumulatedList = new ArrayList<>();
    }

    public InstanceNameAccumulatingAnalyzer(TreeReference contextRef) {
        super(contextRef);
        this.accumulatedList = new ArrayList<>();
    }

    @Override
    public void doNormalTreeRefAnalysis(TreeReference treeRef) throws AnalysisInvalidException {
        if (treeRef.getContextType() == TreeReference.CONTEXT_INSTANCE) {
            accumulatedList.add(treeRef.getInstanceName());
        }
    }

    @Override
    XPathAnalyzer initSameTypeAnalyzer() {
        return new InstanceNameAccumulatingAnalyzer();
    }

}

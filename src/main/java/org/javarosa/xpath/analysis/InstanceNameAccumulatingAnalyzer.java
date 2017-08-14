package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;

/**
 * Created by amstone326 on 8/11/17.
 */

public class InstanceNameAccumulatingAnalyzer extends XPathAccumulatingAnalyzer<String> {

    public InstanceNameAccumulatingAnalyzer() {
        super();
        this.accumulatedList = new ArrayList<>();
    }

    @Override
    public void extractTargetValues(TreeReference treeRef) {
        if (treeRef.getContextType() == TreeReference.CONTEXT_INSTANCE) {
            accumulatedList.add(treeRef.getInstanceName());
        }
    }

}

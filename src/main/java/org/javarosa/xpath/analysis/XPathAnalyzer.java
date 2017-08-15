package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by amstone326 on 8/15/17.
 */

public abstract class XPathAnalyzer {

    private TreeReference contextRefToUseDownstream;
    protected boolean resultIsInvalid;

    public void setRootContextRef(TreeReference rootContextRef) {
        this.contextRefToUseDownstream = rootContextRef;
    }

    public boolean hasContextRef() {
        return this.contextRefToUseDownstream != null;
    }

    public TreeReference getRootContextRef() {
        return this.contextRefToUseDownstream;
    }

    public void invalidateResults() {
        // Something happened in the course of our analysis to indicate that we may not be able
        // to do a complete/accurate analysis, so we should not consider the results valid
        this.resultIsInvalid = true;
    }

    public void doAnalysis(XPathAnalyzable analyzable) {
        // So that the default behavior is to do nothing
    }

    public void doAnalysis(TreeReference treeReference) {
        // Overridden by InstanceNameAccumulatingAnalyzer
    }
}

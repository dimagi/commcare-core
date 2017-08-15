package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

/**
 * Created by amstone326 on 8/15/17.
 */

public abstract class XPathAnalyzer {

    private TreeReference originalContextRef;
    private TreeReference contextRef;

    public XPathAnalyzer() {

    }

    public XPathAnalyzer(TreeReference contextRef) {
        this.contextRef = contextRef;
    }

    public TreeReference getContext() {
        return this.contextRef;
    }

    public TreeReference getOriginalContext() {
        if (this.originalContextRef != null) {
            return this.originalContextRef;
        }
        // Means that we only have 1 level of context
        return this.contextRef;
    }

    public void doAnalysis(XPathAnalyzable analyzable) throws AnalysisInvalidException {
        // So that the default behavior is to do nothing
    }

    public void doAnalysis(TreeReference ref) throws AnalysisInvalidException {
        if (ref.getContextType() == TreeReference.CONTEXT_INHERITED) {
            doAnalysisForRelativeTreeRef(ref);
        } else if (ref.getContextType() == TreeReference.CONTEXT_ORIGINAL) {
            doAnalysisForTreeRefWithCurrent(ref);
        } else {
            doNormalTreeRefAnalysis(ref);
        }
    }

    public void doNormalTreeRefAnalysis(TreeReference treeReference)
            throws AnalysisInvalidException {
        // So that the default behavior is to do nothing
    }

    public void doAnalysisForTreeRefWithCurrent(TreeReference expressionWithContextTypeCurrent)
            throws AnalysisInvalidException {
        // So that the default behavior is to do nothing
    }

    public void doAnalysisForRelativeTreeRef(TreeReference expressionWithContextTypeRelative)
            throws AnalysisInvalidException {
        // So that the default behavior is to do nothing
    }
}

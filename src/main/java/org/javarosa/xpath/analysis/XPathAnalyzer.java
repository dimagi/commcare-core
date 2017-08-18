package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * An XPathAnalyzer is an object that can perform static analysis of any XPathAnalyzable
 * (an XPathExpression or TreeReference) to ascertain specific semantic information about the
 * raw content of the expression string itself
 *
 * @author Aliza Stone
 */
public abstract class XPathAnalyzer {

    private TreeReference originalContextRef;
    private TreeReference contextRef;
    protected List<XPathAnalyzer> subAnalyzers;
    protected boolean isSubAnalyzer;

    public XPathAnalyzer() {
        this.subAnalyzers = new ArrayList<>();
    }

    public XPathAnalyzer(TreeReference contextRef) {
        this();
        this.contextRef = contextRef;
    }

    public TreeReference getContextRef() {
        return this.contextRef;
    }

    public TreeReference getOriginalContextRef() {
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

    public XPathAnalyzer spawnSubAnalyzer(TreeReference subContext) {
        XPathAnalyzer subAnalyzer = initSameTypeAnalyzer();
        subAnalyzer.isSubAnalyzer = true;
        subAnalyzer.originalContextRef = this.getOriginalContextRef();
        subAnalyzer.contextRef = subContext;
        this.subAnalyzers.add(subAnalyzer);
        return subAnalyzer;
    }

    abstract XPathAnalyzer initSameTypeAnalyzer();

}

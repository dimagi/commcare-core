package org.javarosa.xpath.analysis;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathFuncExpr;

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
    protected boolean shortCircuit = false;

    public XPathAnalyzer() {
        this.subAnalyzers = new ArrayList<>();
    }

    protected void setContext(EvaluationContext context) {
        setContext(context.getContextRef(), context.getOriginalContext());
    }

    protected void setContext(TreeReference contextRef) {
        setContext(contextRef, null);
    }

    protected void setContext(TreeReference contextRef, TreeReference originalContextRef) {
        this.contextRef = contextRef;
        this.originalContextRef = originalContextRef;
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

    protected void requireOriginalContext(TreeReference forReference) throws AnalysisInvalidException{
        if (getOriginalContextRef() == null) {
            throw new AnalysisInvalidException("No original context ref was available when " +
                    "trying to analyze the following expression with context type current: " +
                    forReference.toString());
        }
    }

    protected void requireContext(TreeReference forReference) throws AnalysisInvalidException{
        if (getContextRef() == null) {
            throw new AnalysisInvalidException("No context ref was available when trying to " +
                    "analyze the following expression with context type relative: " +
                    forReference.toString());
        }
    }

    public void doAnalysis(XPathAnalyzable analyzable) throws AnalysisInvalidException {
        // So that the default behavior is to do nothing
    }

    // TODO: There should be special handling for references that contain "../" as well
    public void doAnalysis(TreeReference ref) throws AnalysisInvalidException {
        if (ref.getContextType() == TreeReference.CONTEXT_INHERITED) {
            doAnalysisForRelativeTreeRef(ref);
        } else if (ref.getContextType() == TreeReference.CONTEXT_ORIGINAL) {
            doAnalysisForTreeRefWithCurrent(ref);
        } else {
            doNormalTreeRefAnalysis(ref);
        }
    }

    // This implementation should work for most analyzers, but some subclasses may want to override
    // and provide more specific behavior
    public void doAnalysisForTreeRefWithCurrent(TreeReference expressionWithContextTypeCurrent)
            throws AnalysisInvalidException {
        requireOriginalContext(expressionWithContextTypeCurrent);
        doNormalTreeRefAnalysis(expressionWithContextTypeCurrent.contextualize(getOriginalContextRef()));
    }

    // This implementation should work for most analyzers, but some subclasses may want to override
    // and provide more specific behavior
    public void doAnalysisForRelativeTreeRef(TreeReference expressionWithContextTypeRelative)
            throws AnalysisInvalidException {
        requireContext(expressionWithContextTypeRelative);
        doNormalTreeRefAnalysis(expressionWithContextTypeRelative.contextualize(this.getContextRef()));
    }

    public void doNormalTreeRefAnalysis(TreeReference treeReference)
            throws AnalysisInvalidException {
        // So that we can override in subclasses for which this is relevant
    }

    public void doAnalysis(XPathFuncExpr expr) {
        // So that we can override in subclasses for which this is relevant
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

    public boolean shortCircuit() {
        return shortCircuit;
    }

}

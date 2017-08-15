package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.annotations.Nullable;

/**
 * Created by amstone326 on 8/11/17.
 */

public abstract class XPathAccumulatingAnalyzer<T> extends XPathAnalyzer {

    protected List<T> accumulatedList;

    public XPathAccumulatingAnalyzer() {

    }

    public XPathAccumulatingAnalyzer(TreeReference contextRef) {
        super(contextRef);
    }

    @Nullable
    public List<T> accumulateAsList(XPathAnalyzable rootExpression) {
        try {
            rootExpression.applyAndPropagateAnalyzer(this);
            return accumulatedList;
        } catch (AnalysisInvalidException e) {
            return null;
        }
    }

    @Nullable
    public Set<T> accumulateAsSet(XPathAnalyzable rootExpression) {
        try {
            rootExpression.applyAndPropagateAnalyzer(this);
            return convertResultToSet();
        } catch (AnalysisInvalidException e) {
            return null;
        }
    }

    private Set<T> convertResultToSet() {
        Set<T> set = new HashSet<>();
        for (T item : accumulatedList) {
            set.add(item);
        }
        return set;
    }

    // For all AccumulatingAnalyzers, it is sufficient to handle a current() reference by
    // applying the analyzer separately to the expression itself and to the context ref
    @Override
    public void doAnalysisForTreeRefWithCurrent(TreeReference expressionWithContextTypeCurrent)
            throws AnalysisInvalidException {

        if (getOriginalContext() == null) {
            throw new AnalysisInvalidException();

        }

        doNormalTreeRefAnalysis(expressionWithContextTypeCurrent);
        getOriginalContext().applyAndPropagateAnalyzer(this);
    }

    // For all AccumulatingAnalyzers, it is sufficient to handle a relative reference by
    // applying the analyzer separately to the expression itself and to the context ref
    @Override
    public void doAnalysisForRelativeTreeRef(TreeReference expressionWithContextTypeRelative)
            throws AnalysisInvalidException {

        if (getContext() == null) {
            throw new AnalysisInvalidException();
        }

        doNormalTreeRefAnalysis(expressionWithContextTypeRelative);
        getContext().applyAndPropagateAnalyzer(this);
    }

}

package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.annotations.Nullable;

/**
 * A type of XPathAnalyzer which collects and aggregates a specified type of information from
 * wherever it is present in the expression.
 *
 * IMPORTANT NOTE: An accumulating analyzer may analyze the same sub-expression or context ref of
 * an expression multiple times in a single analysis pass. This means:
 * - An AccumulatingAnalyzer is NOT appropriate to use for answering questions such as
 * "How many times is X referenced in this expression?"
 * - An accumulating analyzer IS appropriate to use for answering questions such as
 * "What is the set of all things of X type which are referenced at least one time in this expression?"
 *
 * @author Aliza Stone
 */
public abstract class XPathAccumulatingAnalyzer<T> extends XPathAnalyzer {

    private Collection<T> accumulated;

    void addToResult(T t) {
        accumulated.add(t);
    }

    int size() {
        return accumulated.size();
    }

    @Override
    public XPathAnalyzer spawnSubAnalyzer(TreeReference subContext) {
        XPathAccumulatingAnalyzer subAnalyzer =
                (XPathAccumulatingAnalyzer)super.spawnSubAnalyzer(subContext);
        subAnalyzer.accumulated = this.accumulated instanceof Set ? new HashSet<>() : new ArrayList<>();
        return subAnalyzer;


    }

    @Nullable
    public Set<T> accumulate(XPathAnalyzable rootExpression) {
        try {
            accumulated = new HashSet<>();
            rootExpression.applyAndPropagateAnalyzer(this);
            Set<T> resultSet = new HashSet<>();
            aggregateResults(resultSet);
            return resultSet;
        } catch (AnalysisInvalidException e) {
            return null;
        }
    }

    // FOR TESTING PURPOSES ONLY -- This cannot be relied upon to not return duplicates
    @Nullable
    public List<T> accumulateAsList(XPathAnalyzable rootExpression) {
        try {
            accumulated = new ArrayList<>();
            rootExpression.applyAndPropagateAnalyzer(this);
            List<T> resultList = new ArrayList<>();
            aggregateResults(resultList);
            return resultList;
        } catch (AnalysisInvalidException e) {
            return null;
        }
    }

    private void aggregateResults(Collection<T> resultCollection) {
        resultCollection.addAll(this.accumulated);
        for (XPathAnalyzer subAnalyzer : this.subAnalyzers) {
            ((XPathAccumulatingAnalyzer)subAnalyzer).aggregateResults(resultCollection);
        }
    }

}

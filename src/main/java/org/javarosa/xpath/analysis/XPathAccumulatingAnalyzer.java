package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.ArrayList;
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

    private List<T> accumulatedList = new ArrayList<>();

    @Nullable
    public Set<T> accumulate(XPathAnalyzable rootExpression) {
        try {
            rootExpression.applyAndPropagateAnalyzer(this);
            Set<T> set = new HashSet<>();
            set.addAll(aggregateResults(new ArrayList<T>()));
            return set;
        } catch (AnalysisInvalidException e) {
            return null;
        }
    }

    protected void addResultToList(T t) {
        accumulatedList.add(t);
    }

    private List<T> aggregateResults(List<T> aggregated) {
        aggregated.addAll(this.accumulatedList);
        for (XPathAnalyzer subAnalyzer : this.subAnalyzers) {
            ((XPathAccumulatingAnalyzer)subAnalyzer).aggregateResults(aggregated);
        }
        return aggregated;
    }

    // FOR TESTING PURPOSES ONLY -- This cannot be relied upon to not return duplicates
    @Nullable
    public List<T> accumulateAsList(XPathAnalyzable rootExpression) {
        try {
            rootExpression.applyAndPropagateAnalyzer(this);
            return aggregateResults(new ArrayList<T>());
        } catch (AnalysisInvalidException e) {
            return null;
        }
    }

}

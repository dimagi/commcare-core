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

    @Nullable
    public List<T> accumulateAsList(XPathAnalyzable rootExpression) {
        rootExpression.applyAndPropagateAnalyzer(this);
        if (this.resultIsInvalid) {
            return null;
        }
        return accumulatedList;
    }

    @Nullable
    public Set<T> accumulateAsSet(XPathAnalyzable rootExpression) {
        rootExpression.applyAndPropagateAnalyzer(this);
        if (this.resultIsInvalid) {
            return null;
        }
        return convertResultToSet();
    }

    private Set<T> convertResultToSet() {
        Set<T> set = new HashSet<>();
        for (T item : accumulatedList) {
            set.add(item);
        }
        return set;
    }

}

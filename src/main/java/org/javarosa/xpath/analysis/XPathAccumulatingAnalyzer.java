package org.javarosa.xpath.analysis;

import org.javarosa.core.model.instance.TreeReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by amstone326 on 8/11/17.
 */

public abstract class XPathAccumulatingAnalyzer<T> {

    protected List<T> accumulatedList;

    public void extractTargetValues(XPathAnalyzable analyzable) {
        // so that the default behavior is to do nothing
    }

    public void extractTargetValues(TreeReference treeRef) {
        // so that InstanceNameAccumulatingAnalyzer can override this
    }

    public List<T> accumulateAsList(XPathAnalyzable rootExpression) {
        rootExpression.applyAndPropagateAccumulatingAnalyzer(this);
        return accumulatedList;
    }

    public Set<T> accumulateAsSet(XPathAnalyzable rootExpression) {
        rootExpression.applyAndPropagateAccumulatingAnalyzer(this);
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

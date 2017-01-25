package org.javarosa.core.model.trace;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Vector;

/**
 * Created by ctsims on 1/24/2017.
 */

public class BulkEvaluationTrace extends EvaluationTrace {

    boolean bulkEvaluationSucceeded = false;
    long runtimeImpact = 0;
    String predicatesCovered;
    String outputValue;

    /**
     * Creates an initial record for a bulk trace
     */
    public BulkEvaluationTrace() {
        super("");
    }

    /**
     * Set the outcome value of this evaluation step
     *
     * @param value set the outcome of evaluating this expression
     */
    public void setOutcome(Object value) {
        throw new RuntimeException("Bulk evaluation shouldn't have set outcome called on it");
    }

    public void setEvaluatedPredicates(Vector<XPathExpression> startingSet,
                                       Vector<XPathExpression> finalSet,
                                       Vector<TreeReference> childSet) {
        this.triggerExprComplete();

        if(startingSet == null) {
            bulkEvaluationSucceeded = false;
            return;
        }
        int predicatesCounted = startingSet.size() - finalSet.size();
        if(predicatesCounted == 0) {
            bulkEvaluationSucceeded = false;
            return;
        }

        bulkEvaluationSucceeded = true;

        String predicatesCovered = "";

        for(int i = 0 ; i < predicatesCounted ; ++i) {
            predicatesCovered += "[" + startingSet.get(i).toPrettyString() + "]";
        }

        this.predicatesCovered = predicatesCovered;

        this.outputValue = "Results: " + childSet.size();
    }

    public String getExpression() {
        return predicatesCovered;
    }

    public String getValue() {
        return outputValue;
    }

    public boolean isBulkEvaluationSucceeded() {
        return bulkEvaluationSucceeded;
    }

}

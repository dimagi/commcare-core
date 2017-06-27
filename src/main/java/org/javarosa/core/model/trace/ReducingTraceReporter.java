package org.javarosa.core.model.trace;

import org.javarosa.core.util.OrderedHashtable;

import java.util.HashMap;
import java.util.Vector;

/**
 * A Cummulative trace reporter collects and "folds" traces which execute over multiple elements.
 * It is helpful for identifying how many times different expressions are evaluated, and aggregating
 * elements of each execution
 *
 * Created by ctsims on 1/27/2017.
 */
public class ReducingTraceReporter extends EvaluationTraceReporter {
    OrderedHashtable<String, EvaluationTraceReduction> traceMap = new OrderedHashtable<>();

    @Override
    public boolean wereTracesReported() {
        return false;
    }

    @Override
    public void reportTrace(EvaluationTrace trace) {
        String key = trace.getExpression();
        if(traceMap.containsKey(key)) {
            traceMap.get(trace.getExpression()).foldIn(trace);
        } else {
            traceMap.put(key, new EvaluationTraceReduction(trace));
        }
    }

    @Override
    public void reset() {
        this.traceMap.clear();
    }

    public Vector<EvaluationTrace> getCollectedTraces() {
        return new Vector<EvaluationTrace>(traceMap.values());
    }

}

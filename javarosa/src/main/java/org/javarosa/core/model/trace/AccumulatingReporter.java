package org.javarosa.core.model.trace;

import java.util.Vector;

/**
 * A simple accumulator which keeps track of expressions which are evaluated
 * upon request.
 *
 * Created by ctsims on 10/19/2016.
 */
public class AccumulatingReporter implements EvaluationTraceReporter {
    private final Vector<EvaluationTrace> traces = new Vector<>();

    @Override
    public void reportTrace(EvaluationTrace trace) {
        this.traces.add(trace);
    }

    public Vector<EvaluationTrace> getCollectedTraces() {
        return traces;
    }

    public void clearTraces() {
        this.traces.clear();
    }
}

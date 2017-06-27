package org.javarosa.core.model.trace;

import java.util.Vector;

/**
 * A simple accumulator which keeps track of expressions which are evaluated
 * upon request.
 *
 * Created by ctsims on 10/19/2016.
 */
public class AccumulatingReporter extends EvaluationTraceReporter {
    private final Vector<EvaluationTrace> traces = new Vector<>();

    @Override
    public boolean wereTracesReported() {
        return traces.size() > 0;
    }

    @Override
    public void reportTrace(EvaluationTrace trace) {
        this.traces.add(trace);
    }

    @Override
    public Vector<EvaluationTrace> getCollectedTraces() {
        return traces;
    }

    @Override
    public void reset() {
        this.traces.clear();
    }

}

package org.javarosa.core.model.trace;

import java.util.Vector;

/**
 * A trace reporter provides a callback interface to allow for an
 * evaluation context to callback expression trace results directly in debug
 * mode, rather than requiring them to be requested.
 *
 * Created by ctsims on 10/19/2016.
 */
public abstract class EvaluationTraceReporter {

    abstract boolean wereTracesReported();

    public abstract void reportTrace(EvaluationTrace trace);

    abstract void reset();

    abstract Vector<EvaluationTrace> getCollectedTraces();

    public void printAndClearTraces(String description) {
        if (wereTracesReported()) {
            System.out.println(description);
        }

        StringEvaluationTraceSerializer serializer = new StringEvaluationTraceSerializer();

        for (EvaluationTrace trace : getCollectedTraces()) {
            System.out.println(trace.getExpression() + ": " + trace.getValue());
            System.out.print(serializer.serializeEvaluationLevels(trace));
        }

        reset();
    }
}

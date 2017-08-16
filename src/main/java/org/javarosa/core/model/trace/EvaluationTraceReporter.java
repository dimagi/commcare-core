package org.javarosa.core.model.trace;

import java.util.Vector;

/**
 * A trace reporter provides a callback interface to allow for an
 * evaluation context to callback expression trace results directly in debug
 * mode, rather than requiring them to be requested.
 *
 * Created by ctsims on 10/19/2016.
 */
public interface EvaluationTraceReporter {

    boolean wereTracesReported();

    void reportTrace(EvaluationTrace trace);

    void reset();

    Vector<EvaluationTrace> getCollectedTraces();

}

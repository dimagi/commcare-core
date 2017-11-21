package org.javarosa.core.model.utils;

import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.trace.StringEvaluationTraceSerializer;

/**
 * Utility functions for instrumentation in the engine
 *
 * Created by ctsims on 7/6/2017.
 */

public class InstrumentationUtils {

    /**
     * Prints out traces (if any exist) from the provided reporter with a description into sysout
     */
    public static void printAndClearTraces(EvaluationTraceReporter reporter, String description) {
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                System.out.println(description);
            }

            StringEvaluationTraceSerializer serializer = new StringEvaluationTraceSerializer();

            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                System.out.println(trace.getExpression() + ": " + trace.getValue());
                System.out.print(serializer.serializeEvaluationLevels(trace));
            }

            reporter.reset();
        }
    }
}

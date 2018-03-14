package org.javarosa.core.model.utils;

import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.trace.EvaluationTraceSerializer;

/**
 * Utility functions for instrumentation in the engine
 *
 * Created by ctsims on 7/6/2017.
 */

public class InstrumentationUtils {


    public static void printAndClearTraces(EvaluationTraceReporter reporter, String description) {
        printAndClearTraces(reporter, description, EvaluationTraceSerializer.TraceInfoType.FULL_PROFILE);
    }

    /**
     * Prints out traces (if any exist) from the provided reporter with a description into sysout
     */
    public static void printAndClearTraces(EvaluationTraceReporter reporter, String description,
                                           EvaluationTraceSerializer.TraceInfoType requestedInfo) {
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                System.out.println(description);
            }

            EvaluationTraceSerializer serializer = new EvaluationTraceSerializer();

            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                System.out.println(trace.getExpression() + ": " + trace.getValue());
                System.out.print(serializer.serializeEvaluationLevels(trace, requestedInfo));
            }

            reporter.reset();
        }
    }

    /**
     * Prints out traces (if any exist) from the provided reporter with a description into sysout
     */
    public static String collectAndClearTraces(EvaluationTraceReporter reporter, String description,
                                               EvaluationTraceSerializer.TraceInfoType requestedInfo) {
        String returnValue = "";
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                returnValue += description + "\n";
            }

            EvaluationTraceSerializer serializer = new EvaluationTraceSerializer();

            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                returnValue += trace.getExpression() + ": " + trace.getValue()  + "\n";
                returnValue += serializer.serializeEvaluationLevels(trace, requestedInfo);
            }

            reporter.reset();
        }
        return returnValue;
    }

}

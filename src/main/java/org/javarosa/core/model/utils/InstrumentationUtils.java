package org.javarosa.core.model.utils;

import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.trace.TraceSerialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility functions for instrumentation in the engine
 *
 * Created by ctsims on 7/6/2017.
 */

public class InstrumentationUtils {

    public static void printAndClearTraces(EvaluationTraceReporter reporter, String description) {
        printAndClearTraces(reporter, description, TraceSerialization.TraceInfoType.FULL_PROFILE);
    }

    /**
     * Prints out traces (if any exist) from the provided reporter with a description into sysout
     */
    public static void printAndClearTraces(EvaluationTraceReporter reporter, String description,
                                           TraceSerialization.TraceInfoType requestedInfo) {
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                System.out.println(description);
            }

            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                System.out.println(trace.getExpression() + ": " + trace.getValue());
                System.out.print(TraceSerialization.serializeEvaluationTrace(trace, requestedInfo,
                        reporter.reportAsFlat()));
            }

            reporter.reset();
        }
    }

    /**
     * Prints out traces (if any exist) from the provided reporter with a description into sysout
     */
    public static String collectAndClearTraces(EvaluationTraceReporter reporter, String description,
                                               TraceSerialization.TraceInfoType requestedInfo) {
        String result = collectTraces(reporter, description, requestedInfo);
        if (reporter != null) {
            reporter.reset();
        }
        return result;
    }

    public static String collectTraces(EvaluationTraceReporter reporter, String description,
                                       TraceSerialization.TraceInfoType requestedInfo) {
        String returnValue = "";
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                returnValue += description + "\n";
            }

            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                returnValue += trace.getExpression() + ": " + trace.getValue()  + "\n";
                returnValue += TraceSerialization.serializeEvaluationTrace(trace, requestedInfo,
                        reporter.reportAsFlat());
            }
        }
        return returnValue;
    }

    public static List<String> getMatchedTraces(EvaluationTraceReporter reporter, Predicate<String> predicate) {
        String output = collectTraces(reporter, "", TraceSerialization.TraceInfoType.FULL_PROFILE);
        return Arrays.stream(output.split("\n")).filter(predicate).collect(Collectors.toList());
    }

    public static int countMatchedTraces(EvaluationTraceReporter reporter, Predicate<String> predicate) {
        return getMatchedTraces(reporter, predicate).size();
    }

    public static void printExpressionsThatUsedCaching(EvaluationTraceReporter reporter, String description) {
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                System.out.println(description);
            }

            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                if (trace.evaluationUsedExpressionCache()) {
                    System.out.println(trace.getExpression() + ": " + trace.getValue());
                    System.out.println("    " + trace.getCacheReport());
                }
            }
        }
    }

    public static void printCachedAndNotCachedExpressions(EvaluationTraceReporter reporter, String description) {
        if (reporter != null) {
            if (reporter.wereTracesReported()) {
                System.out.println(description);
            }

            List<EvaluationTrace> withCaching = new ArrayList<>();
            List<EvaluationTrace> withoutCaching = new ArrayList<>();
            for (EvaluationTrace trace : reporter.getCollectedTraces()) {
                if (trace.evaluationUsedExpressionCache()) {
                    withCaching.add(trace);
                } else {
                    withoutCaching.add(trace);
                }
            }

            System.out.println("EXPRESSIONS NEVER CACHED: " + withoutCaching.size());
            for (EvaluationTrace trace : withoutCaching) {
                System.out.println(trace.getExpression() + ": " + trace.getValue());
            }

            System.out.println("EXPRESSIONS CACHED: " + withCaching.size());
            for (EvaluationTrace trace : withCaching) {
                System.out.println(trace.getExpression() + ": " + trace.getValue());
                System.out.println("    " + trace.getCacheReport());
            }

            reporter.reset();
        }
    }

}

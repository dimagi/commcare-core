package org.javarosa.core.model.trace;

/**
 * Serializes an evaluation trace into a raw string for command line or other
 * debugging purposes.
 *
 * @author ctsims
 */
public class EvaluationTraceSerializer {

    private static final String ONE_INDENT = "    ";

    public enum TraceInfoType {
        FULL_PROFILE,
        CACHE_INFO_ONLY
    }

    public String serializeEvaluationTrace(EvaluationTrace input, TraceInfoType requestedInfo,
                                           boolean serializeFlat) {
        return dumpExprOutput(input, 1, requestedInfo, serializeFlat);
    }

    private String dumpExprOutput(EvaluationTrace level, int refLevel, TraceInfoType requestedInfo,
                                  boolean serializeFlat) {
        String output = serializeFlat ?
                addDesiredData(level, requestedInfo, "", ONE_INDENT) :
                indentExprAndAddData(level, refLevel, requestedInfo);

        if (!serializeFlat) {
            for (EvaluationTrace child : level.getSubTraces()) {
                output += dumpExprOutput(child, refLevel + 1, requestedInfo, false) + "\n";
            }
        }

        return output;
    }

    private String indentExprAndAddData(EvaluationTrace level, int indentLevel,
                                        TraceInfoType requestedInfo) {
        String expr = level.getExpression();
        String value = level.getValue();

        String indent = "";
        for (int i = 0; i < indentLevel; ++i) {
            indent += ONE_INDENT;
        }

        return addDesiredData(level, requestedInfo, indent + expr + ": " + value + "\n", indent);
    }

    private String addDesiredData(EvaluationTrace level, TraceInfoType requestedInfo,
                                  String coreString, String indent) {
        String newResult = coreString;
        switch (requestedInfo) {
            case FULL_PROFILE:
                String profile = level.getProfileReport();
                if (profile != null) {
                    for (String profileLine : profile.split("\\n")) {
                        newResult += indent + profileLine + "\n";
                    }
                }
                break;
            case CACHE_INFO_ONLY:
                newResult += indent + level.getCacheReport() + "\n";
                break;
        }
        return newResult;
    }
}

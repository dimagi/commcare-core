package org.javarosa.core.model.trace;

/**
 * Serializes an evaluation trace into a raw string for command line or other
 * debugging purposes.
 *
 * @author ctsims
 */
public class EvaluationTraceSerializer {

    public enum TraceInfoType {
        FULL_PROFILE,
        CACHE_INFO_ONLY
    }

    public String serializeEvaluationLevels(EvaluationTrace input, TraceInfoType requestedInfo) {
        return dumpExprOutput(input, 1, requestedInfo);
    }

    private String dumpExprOutput(EvaluationTrace level, int refLevel, TraceInfoType requestedInfo) {
        String output = indentExprAndValue(level, refLevel, requestedInfo);
        for (EvaluationTrace child : level.getSubTraces()) {
            output += dumpExprOutput(child, refLevel + 1, requestedInfo) + "\n";
        }
        return output;
    }

    private String indentExprAndValue(EvaluationTrace level, int indentLevel, TraceInfoType requestedInfo) {
        String expr = level.getExpression();
        String value = level.getValue();

        String indent = "";
        for (int i = 0; i < indentLevel; ++i) {
            indent += "    ";
        }

        return addDesiredData(level, requestedInfo, indent + expr + ": " + value + "\n", indent);
    }

    private String addDesiredData(EvaluationTrace level, TraceInfoType requestedInfo, String coreString, String indent) {
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

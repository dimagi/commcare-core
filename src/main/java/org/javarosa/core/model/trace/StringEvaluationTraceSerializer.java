package org.javarosa.core.model.trace;

/**
 * Serializes an evaluation trace into a raw string for command line or other
 * debugging purposes.
 *
 * @author ctsims
 */
public class StringEvaluationTraceSerializer implements EvaluationTraceSerializer<String> {

    @Override
    public String serializeEvaluationLevels(EvaluationTrace input) {
        return dumpExprOutput(input, 1);
    }

    private String dumpExprOutput(EvaluationTrace level, int refLevel) {
        String output = indentExprAndValue(level, refLevel);
        for (EvaluationTrace child : level.getSubTraces()) {
            output += dumpExprOutput(child, refLevel + 1) + "\n";
        }
        return output;
    }

    private String indentExprAndValue(EvaluationTrace level, int indentLevel) {
        String expr = level.getExpression();
        String value = level.getValue();
        String profile = level.getProfileReport();

        String indent = "";
        for (int i = 0; i < indentLevel; ++i) {
            indent += "    ";
        }

        return addProfileData(indent + expr + ": " + value + "\n", profile, indent);
    }

    private String addProfileData(String coreString, String profile, String indent) {
        if(profile == null) {
            return coreString;
        } else {
            String newResult = coreString;
            for(String profileLine : profile.split("\\n")) {
                newResult += indent + profileLine + "\n";
            }
            return newResult;
        }
    }
}

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
        String output = indentExprAndValue(level.getExpression(), level.getValue(), refLevel);
        for (EvaluationTrace child : level.getSubTraces()) {
            output += dumpExprOutput(child, refLevel + 1) + "\n";
        }
        return output;
    }

    private String indentExprAndValue(String expr, String value, int indentLevel) {
        String indent = "";
        for (int i = 0; i < indentLevel; ++i) {
            indent += "    ";
        }

        return indent + expr + ": " + value + "\n";
    }
}

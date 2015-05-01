package org.javarosa.core.model.trace;

/**
 * Serializes an evaluation trace into a raw string for command line or other debugging purposes.
 * 
 * @author ctsims
 *
 */
public class StringEvaluationTraceSerializer implements EvaluationTraceSerializer<String> {

    public String serializeEvaluationLevels(EvaluationTrace input) {
        return dumpExprOutput(input, 1);
    }

    public String dumpExprOutput(EvaluationTrace level, int refLevel) {
        String output = String.format("%s%s: %s", tabLevel(refLevel),
                level.getExpression(), level.getValue())
                + "\n";
        for (EvaluationTrace child : level.getSubTraces()) {
            output += dumpExprOutput(child, refLevel + 1) + "\n";
        }
        return output;
    }

    public String tabLevel(int tabLevel) {
        String level = "";
        for (int i = 0; i < tabLevel; ++i) {
            level += "    ";
        }
        return level;
    }

}

package org.javarosa.xpath.expr;


import org.commcare.cases.util.StringUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class XPathChecksumFunc extends XPathFuncExpr {
    public static final String NAME = "checksum";
    private static final int EXPECTED_ARG_COUNT = 2;

    public static final String ALGORITHM_KEY_VERHOEFF = "verhoeff";

    public XPathChecksumFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathChecksumFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return checksum(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * @param o1 algorithm type used to calculate checksum. We only support 'verhoeff' for now.
     * @param o2 input we are calculating checksum for
     * @return checksum of {@code o2} calculated using  {@code o1} type algorithm
     */
    private static String checksum(Object o1, Object o2) {
        String algorithmKey = FunctionUtils.toString(o1);
        String input = FunctionUtils.toString(o2);

        switch (algorithmKey) {
            case ALGORITHM_KEY_VERHOEFF:
                return verhoeffChecksum(input);
            default:
                throw new XPathUnsupportedException("Bad algorithm key " + algorithmKey + ". We only support 'verhoeff' as algorithm key right now.");
        }
    }

    /**
     * Calculates Verhoeff checksum for the given {@code input} string <p>
     *
     * @param input input string to calculate verhoeff checksum for
     * @return Verhoeff checksum value for {@code input}
     * @see <a href="https://en.wikibooks.org/wiki/Algorithm_Implementation/Checksums/Verhoeff_Algorithm#Java">Based on Verhoeff checksum implementation here</a>
     */
    private static String verhoeffChecksum(String input) {

        // The multiplication table
        int[][] op = new int[][]{
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
                {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
                {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
                {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
                {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
                {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
                {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
                {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
                {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
        };

        // The permutation table
        int[][] p = new int[][]{
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
                {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
                {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
                {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
                {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
                {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
                {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
                {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
        };

        // The inverse table
        int[] inv = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

        ArrayList<Character> inputList = StringUtils.toList(input);
        Collections.reverse(inputList);

        int check = 0;
        for (int i = 0; i < inputList.size(); i++) {
            check = op[check][p[((i + 1) % 8)][Character.getNumericValue(inputList.get(i))]];
        }

        return Integer.toString(inv[check]);
    }

}

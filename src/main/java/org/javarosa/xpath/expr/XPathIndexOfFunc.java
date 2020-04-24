package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Identifies the numerical index of the provided argument into the provided sequence, if
 * it is a member of the sequence. If not, an empty result is returned.
 *
 * Created by ctsims on 04/24/2020
 */

public class XPathIndexOfFunc extends XPathFuncExpr {
    public static final String NAME = "index-of";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathIndexOfFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathIndexOfFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        Object[] argList = FunctionUtils.getSequence(evaluatedArgs[0]);
        String indexedItem = FunctionUtils.toString(evaluatedArgs[1]);

        for(int i = 0 ; i < argList.length ; ++i) {
            if(argList[i].equals(indexedItem)) {
                return i;
            }
        }
        return "";
    }



}

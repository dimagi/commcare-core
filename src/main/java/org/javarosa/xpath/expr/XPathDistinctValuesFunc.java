package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Implements distinct-values against a nodeset input.
 *
 * Will return a sequence with no duplicate values. Note that sequences are only currently
 * supported provisionally by a limited number of methods.
 *
 * This method will currently _not_ perform type inference in a meaningful way. Values are compared
 * through simple string equality in their current form.
 *
 * Created by ctsims on 11/14/2017.
 */

public class XPathDistinctValuesFunc extends XPathFuncExpr {
    public static final String NAME = "distinct-values";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathDistinctValuesFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDistinctValuesFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        if (!(evaluatedArgs[0] instanceof XPathNodeset)) {
            throw new XPathTypeMismatchException("distinct-values requires a nodeset input, instead, argument" +
                    " is " + evaluatedArgs);
        }
        XPathNodeset input = (XPathNodeset)evaluatedArgs[0];
        Object[] list = input.toArgList();
        HashSet<String> returnSet = new LinkedHashSet<>();
        for (Object o : list) {
            returnSet.add(FunctionUtils.toString(o));
        }
        return returnSet.toArray();
    }



}

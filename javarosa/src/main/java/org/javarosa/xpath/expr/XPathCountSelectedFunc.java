package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathCountSelectedFunc extends XPathFuncExpr {
    private static final String NAME = "count-selected";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCountSelectedFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCountSelectedFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return countSelected(evaluatedArgs[0]);
    }

    /**
     * return the number of choices in a multi-select answer
     *
     * @param o XML-serialized answer to multi-select question (i.e, space-delimited choice values)
     */
    public static Double countSelected(Object o) {

        Object evalResult = unpack(o);
        if (!(evalResult instanceof String)) {
            throw new XPathTypeMismatchException("count-selected argument was not a select list");
        }

        String s = (String)evalResult;
        return new Double(DataUtil.splitOnSpaces(s).length);
    }

}

package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSelectedAtFunc extends XPathFuncExpr {
    public static final String NAME = "selected-at";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSelectedAtFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSelectedAtFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return selectedAt(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * Get the Nth item in a selected list
     *
     * @param o1 XML-serialized answer to multi-select question (i.e, space-delimited choice values)
     * @param o2 the integer index into the list to return
     */
    private static String selectedAt(Object o1, Object o2) {
        String selection = (String)FunctionUtils.unpack(o1);
        int index = FunctionUtils.toInt(o2).intValue();

        String[] entries = DataUtil.splitOnSpaces(selection);

        if (entries.length <= index) {
            throw new XPathException("Attempting to select element " + index +
                    " of a list with only " + entries.length + " elements.");
        } else {
            return entries[index];
        }
    }

}

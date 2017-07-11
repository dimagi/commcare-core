package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.spec.XPathType;

/**
 * Created by amstone326 on 6/28/17.
 */
public class XPathSortFunc extends XPathFuncExpr {

    public static final String NAME = "sort";

    // since we accept 1-2 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathSortFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSortFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 1 || args.length > 2) {
            throw new XPathArityException(name, "1 or 2 arguments", args.length);
        }
    }

    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        List<String> sortedList;
        if (evaluatedArgs.length == 1) {
            sortedList = sortSingleList(FunctionUtils.toString(evaluatedArgs[0]), true);
        } else {
            sortedList = sortSingleList(FunctionUtils.toString(evaluatedArgs[0]),
                    FunctionUtils.toBoolean(evaluatedArgs[1]));
        }
        return DataUtil.listToString(sortedList);
    }

    protected  static List<String> sortSingleList(String spaceSeparatedString, boolean ascending) {
        List<String> items = DataUtil.stringToList(spaceSeparatedString);
        sortSingleList(items, ascending);
        return items;
    }

    protected static void sortSingleList(List<String> items, final boolean ascending) {
        Collections.sort(items, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return (ascending ? 1 : -1) * s1.compareTo(s2);
            }
        });
    }

}

package org.javarosa.xpath.expr;

import org.commcare.modern.util.Pair;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by amstone326 on 7/11/17.
 */

public class XPathSortByFunc extends XPathFuncExpr {

    public static final String NAME = "sort-by";

    // since we accept 2-3 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathSortByFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSortByFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 2 || args.length > 3) {
            throw new XPathArityException(name, "2 or 3 arguments", args.length);
        }
    }

    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        List<String> sortedList;
        if (evaluatedArgs.length == 2) {
            sortedList = sortListByOtherList(FunctionUtils.toString(evaluatedArgs[0]),
                    FunctionUtils.toString(evaluatedArgs[1]), true);
        } else {
            sortedList =
                    sortListByOtherList(FunctionUtils.toString(evaluatedArgs[0]),
                            FunctionUtils.toString(evaluatedArgs[1]),
                            FunctionUtils.toBoolean(evaluatedArgs[2]));
        }
        return DataUtil.listToString(sortedList);
    }

    private List<String> sortListByOtherList(String s1, String s2, final boolean ascending) {
        List<String> targetListItems = DataUtil.stringToList(s1);
        List<String> comparisonListItems = DataUtil.stringToList(s2);

        if (targetListItems.size() != comparisonListItems.size()) {
            throw new XPathTypeMismatchException("Length of lists passed to sort-by() must match, " +
                    "but received lists: " + s1 + " and " + s2);
        }

        List<Pair<String, String>> pairsList =
                createComparisonToTargetPairings(comparisonListItems, targetListItems);

        Collections.sort(pairsList, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> pair1, Pair<String, String> pair2) {
                return (ascending ? 1 : -1) * pair1.first.compareTo(pair2.first);
            }
        });

        List<String> sortedTargetList = new ArrayList<>();
        for (int i = 0; i < pairsList.size(); i++) {
            sortedTargetList.add(pairsList.get(i).second);
        }

        return sortedTargetList;
    }

    private static List<Pair<String, String>> createComparisonToTargetPairings(List<String> comparisonListItems,
                                                                               List<String> targetListItems) {
        List<Pair<String, String>> pairings = new ArrayList<>();
        for (int i = 0; i < comparisonListItems.size(); i++) {
            String comparisonString = comparisonListItems.get(i);
            String targetString = targetListItems.get(i);
            Pair<String, String> pair = new Pair<>(comparisonString, targetString);
            pairings.add(pair);
        }
        return pairings;
    }

}

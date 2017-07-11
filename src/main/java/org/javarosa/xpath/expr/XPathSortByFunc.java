package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private List<String> sortListByOtherList(String s1, String s2, boolean ascending) {
        List<String> targetListItems = DataUtil.stringToList(s1);
        List<String> comparisonListItems = DataUtil.stringToList(s2);

        if (targetListItems.size() != comparisonListItems.size()) {
            throw new XPathTypeMismatchException("Length of lists passed to sort-by() must match, " +
                    "but received lists: " + s1 + " and " + s2);
        }

        Map<String, List<String>> stringMapping =
                createMappingFromComparisonToTarget(comparisonListItems, targetListItems);

        List<String> sortedComparisonList = XPathSortFunc.sortSingleList(s2, ascending);
        List<String> sortedTargetList = new ArrayList<>();
        String previousComparisonString = "";
        for (int i = 0; i < sortedComparisonList.size(); i++) {
            String stringInSortedList = sortedComparisonList.get(i);
            if (stringInSortedList.equals(previousComparisonString)) {
                // Means we already grabbed all the target strings corresponding to this reference string
                continue;
            }
            List<String> correspondingStrings = stringMapping.get(stringInSortedList);
            if (correspondingStrings.size() > 1) {
                XPathSortFunc.sortSingleList(correspondingStrings, ascending);
            }
            sortedTargetList.addAll(correspondingStrings);
            previousComparisonString = stringInSortedList;
        }

        return sortedTargetList;
    }

    private static Map<String, List<String>> createMappingFromComparisonToTarget(List<String> comparisonListItems,
                                                                                 List<String> targetListItems) {
        Map<String, List<String>> stringMapping = new HashMap<>();
        for (int i = 0; i < comparisonListItems.size(); i++) {
            String comparisonString = comparisonListItems.get(i);
            String targetString = targetListItems.get(i);
            List<String> correspondingStrings;
            if (stringMapping.containsKey(comparisonString)) {
                correspondingStrings = stringMapping.get(comparisonString);
            } else {
                correspondingStrings = new ArrayList<>();
                stringMapping.put(comparisonString, correspondingStrings);
            }
            correspondingStrings.add(targetString);
        }
        return stringMapping;
    }

}

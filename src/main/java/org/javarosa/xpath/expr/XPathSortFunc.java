package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
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

    // since we accept 2 or 3 arguments
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
        if (args.length != 2 && args.length != 3) {
            throw new XPathArityException(name, "2 or 3 arguments", args.length);
        }
    }

    @Override
    protected Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        List<String> sortedList;
        if (evaluatedArgs.length == 2) {
            sortedList =
                    sortSingleList(FunctionUtils.toString(evaluatedArgs[0]),
                            FunctionUtils.toBoolean(evaluatedArgs[1]));
        } else {
            sortedList =
                    sortListByOtherList(FunctionUtils.toString(evaluatedArgs[0]),
                            FunctionUtils.toString(evaluatedArgs[1]),
                            FunctionUtils.toBoolean(evaluatedArgs[2]));
        }
        return listToString(sortedList);
    }

    private List<String> sortSingleList(String spaceSeparatedString, final boolean ascending) {
        List<String> items = stringToList(spaceSeparatedString);
        Collections.sort(items, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return (ascending ? 1 : -1) * s1.compareTo(s2);
            }
        });
        return items;
    }

    private List<String> sortListByOtherList(String s1, String s2, boolean ascending) {
        List<String> targetListItems = stringToList(s1);
        List<String> comparisonListItems = stringToList(s2);

        if (targetListItems.size() != comparisonListItems.size()) {
            throw new XPathTypeMismatchException("Length of lists passed to sort() must match");
        }

        Map<String, List<String>> stringMapping =
                createMappingFromComparisonToTarget(comparisonListItems, targetListItems);

        List<String> sortedComparisonList = sortSingleList(s2, ascending);
        List<String> sortedTargetList = new ArrayList<>();
        String previousComparisonString = "";
        for (int i = 0; i < sortedComparisonList.size(); i++) {
            String stringInSortedList = sortedComparisonList.get(i);
            if (stringInSortedList.equals(previousComparisonString)) {
                // Means we already grabbed all the target strings corresponding to this reference string
                continue;
            }
            sortedTargetList.addAll(stringMapping.get(stringInSortedList));
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

    private static List<String> stringToList(String s) {
        return Arrays.asList(s.split(" "));
    }

    private static String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s + " ");
        }
        return sb.toString().substring(0, sb.length()-1);
    }

}

package org.commcare.cases.entity;

import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.Constants;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.util.Comparator;

public class EntitySorter implements Comparator<Entity<TreeReference>> {
    private final DetailField[] detailFields;
    private final boolean reverseSort;
    private final int[] currentSort;
    private boolean hasWarned;
    private EntitySortNotificationInterface notifier;

    public EntitySorter(DetailField[] detailFields, boolean reverseSort, int[] currentSort,
                        EntitySortNotificationInterface notifier) {
        this.detailFields = detailFields;
        this.currentSort = currentSort;
        this.reverseSort = reverseSort;
        this.notifier = notifier;
    }

    @Override
    public int compare(Entity<TreeReference> object1, Entity<TreeReference> object2) {
        for (int aCurrentSort : currentSort) {
            boolean reverse = (detailFields[aCurrentSort].getSortDirection() == DetailField.DIRECTION_DESCENDING) ^ reverseSort;
            int cmp = getCmp(object1, object2, aCurrentSort, reverse);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /**
     * Implemented assuming that the sort direction is DIRECTION_ASCENDING, meaning that:
     *
     * -If object1 < object2, this method should return a negative number
     * -If object1 > object2, this method should return a positive number
     *
     * @param reverse - If true, then the rules above are inverted
     */
    private int getCmp(Entity<TreeReference> object1, Entity<TreeReference> object2, int index,
                       boolean reverse) {
        String a1 = object1.getSortField(index);
        String a2 = object2.getSortField(index);

        // If one of these is null, we need to get the field in the same index, not the field in SortType
        if (a1 == null) {
            a1 = object1.getFieldString(index);
        }
        if (a2 == null) {
            a2 = object2.getFieldString(index);
        }

        boolean showBlanksLast = detailFields[index].showBlanksLastInSort();
        // The user's 'blanks' preference is independent of the specified sort order, so don't
        // factor in the 'reverse' parameter here
        if (a1.equals("")) {
            if (a2.equals("")) {
                return 0;
            } else {
                // a1 is blank and a2 is not
                return showBlanksLast ? 1 : -1;
            }
        } else if (a2.equals("")) {
            // a2 is blank and a1 is not
            return showBlanksLast? -1 : 1;
        }

        int sortType = detailFields[index].getSortType();
        Comparable c1 = applyType(sortType, a1);
        Comparable c2 = applyType(sortType, a2);

        if (c1 == null || c2 == null) {
            // Don't do something smart here, just bail.
            return -1;
        }

        return (reverse ? -1 : 1) * c1.compareTo(c2);
    }

    private Comparable applyType(int sortType, String value) {
        try {
            if (sortType == Constants.DATATYPE_TEXT) {
                return value.toLowerCase();
            } else if (sortType == Constants.DATATYPE_INTEGER) {
                //Double int compares just fine here and also
                //deals with NaN's appropriately

                double ret = FunctionUtils.toInt(value);
                if (Double.isNaN(ret)) {
                    String[] stringArgs = new String[3];
                    stringArgs[2] = value;
                    if (!hasWarned) {
                        notifier.notifyBadfilter(stringArgs);
                        hasWarned = true;
                    }
                }
                return ret;
            } else if (sortType == Constants.DATATYPE_DECIMAL) {
                double ret = FunctionUtils.toDouble(value);
                if (Double.isNaN(ret)) {

                    String[] stringArgs = new String[3];
                    stringArgs[2] = value;
                    if (!hasWarned) {
                        notifier.notifyBadfilter(stringArgs);
                        hasWarned = true;
                    }
                }
                return ret;
            } else {
                //Hrmmmm :/ Handle better?
                return value;
            }
        } catch (XPathTypeMismatchException e) {
            //XPathErrorLogger.INSTANCE.logErrorToCurrentApp(e);
            Logger.exception("Exception when sorting case list.", e);
            e.printStackTrace();
            return null;
        }
    }
}

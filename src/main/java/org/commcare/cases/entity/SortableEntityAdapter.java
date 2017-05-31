package org.commcare.cases.entity;

import org.commcare.suite.model.Detail;
import org.javarosa.core.model.instance.TreeReference;

import java.util.List;

/**
 * Created by amstone326 on 5/24/17.
 */
public abstract class SortableEntityAdapter implements EntitySortNotificationInterface {

    private List<Entity<TreeReference>> entities;
    private Detail detail;
    private int[] currentSort = {};
    private boolean reverseSort = false;
    protected boolean asyncMode = false;

    SortableEntityAdapter(List<Entity<TreeReference>> entityList, Detail detail,
                          boolean asyncMode) {
        this.entities = entityList;
        this.detail = detail;
        this.asyncMode = asyncMode;

        int[] orderedFieldsForSorting = determineFieldsForSortingInOrder();
        if (!this.asyncMode && orderedFieldsForSorting.length != 0) {
            sort(orderedFieldsForSorting);
        }
    }

    private int[] determineFieldsForSortingInOrder() {
        int[] fieldsForSorting = detail.getOrderedFieldIndicesForSorting();
        if (fieldsForSorting.length == 0) {
            for (int i = 0; i < detail.getFields().length; ++i) {
                String header = detail.getFields()[i].getHeader().evaluate();
                if (!"".equals(header)) {
                    fieldsForSorting = new int[]{i};
                    break;
                }
            }
        }
        return fieldsForSorting;
    }

    protected void sort(int[] fields) {
        //The reversing here is only relevant if there's only one sort field and we're on it
        sort(fields, (currentSort.length == 1 && currentSort[0] == fields[0]) && !reverseSort);
    }

    private void sort(int[] fields, boolean reverse) {
        this.reverseSort = reverse;
        currentSort = fields;

        java.util.Collections.sort(this.entities,
                new EntitySorter(detail.getFields(), reverseSort, currentSort, this));
    }

    public int[] getCurrentSort() {
        return currentSort;
    }

    public boolean isCurrentSortReversed() {
        return reverseSort;
    }
}

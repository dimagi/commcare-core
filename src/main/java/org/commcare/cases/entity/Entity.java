package org.commcare.cases.entity;

import org.commcare.cases.util.StringUtils;

import javax.annotation.Nullable;

/**
 * @author ctsims
 */
public class Entity<T> {

    private final T t;
    private Object[] data;
    private String[] sortData;
    private String[] altTextData;
    private boolean[] relevancyData;
    /**
     * Key used to attach external data (i.e. from case list callout) to an entity
     */
    public final String extraKey;
    private boolean shouldReceiveFocus;

    private String groupKey;


    protected Entity(T t, String extraKey) {
        this.t = t;
        this.extraKey = extraKey;
    }

    public Entity(Object[] data, String[] sortData, boolean[] relevancyData, T t,
            String extraKey, boolean shouldReceiveFocus, String groupKey, String[] altTextData) {
        this.t = t;
        this.sortData = sortData;
        this.data = data;
        this.relevancyData = relevancyData;
        this.extraKey = extraKey;
        this.shouldReceiveFocus = shouldReceiveFocus;
        this.groupKey = groupKey;
        this.altTextData = altTextData;
    }

    public Object getField(int i) {
        return data[i];
    }

    /*
     * Same as getField, but guaranteed to return a string.
     * If field is not already a string, will return blank string.
     */
    public String getFieldString(int i) {
        Object field = getField(i);
        if (field instanceof String) {
            return (String)field;
        }
        return "";
    }

    /**
     * @return True iff the given field is relevant and has a non-blank value.
     */
    public boolean isValidField(int fieldIndex) {
        return relevancyData[fieldIndex] && !getField(fieldIndex).equals("");
    }

    /**
     * Gets the indexed field used for searching and sorting these entities
     *
     * @return either the sort or the string field at the provided index, normalized
     * (IE: lowercase, etc) for searching.
     */
    public String getNormalizedField(int i) {
        String normalized = this.getFieldString(i);
        return StringUtils.normalize(normalized);
    }

    public String getSortField(int i) {
        return sortData[i];
    }

    public T getElement() {
        return t;
    }

    public int getNumFields() {
        return data.length;
    }

    public Object[] getData() {
        return data;
    }

    public String[] getAltText() {
        return altTextData;
    }

    public String[] getSortFieldPieces(int i) {
        String sortField = getSortField(i);
        if (sortField == null) {
            return new String[0];
        } else {
            //We always fuzzy match on the sort field and only if it is available
            //(as a way to restrict possible matching)
            sortField = StringUtils.normalize(sortField);
            return sortField.split("\\s+");
        }
    }

    public boolean shouldReceiveFocus() {
        return shouldReceiveFocus;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append("\n").append(i).append("\n");
            sb.append("Data: ").append(data[i]).append("|");
            if (sortData[i] != null) {
                sb.append("SortData: ").append(sortData[i]).append("|");
            }
            sb.append("IsValidField: ").append(isValidField(i));
        }
        return sb.toString() + "\n" + super.toString();
    }

    @Nullable
    public String getGroupKey() {
        return groupKey;
    }
}

package org.commcare.util.screen;

/**
 * Holds essential meta data associated with the entity screen
 */
public class EntityScreenContext {
    private final int mOffSet;
    private final String mSearchText;
    private final int mSortIndex;
    private final int mCasesPerPage;
    private final String[] mSelectedValues;

    /**
     * If requesting a case detail will be a case id, else null. When the case id is given it is used to short
     * circuit the normal TreeReference calculation by inserting a predicate that is [@case_id = <detailSelection>]
     */
    private final String mDetailSelection;

    private static int DEFAULT_CASES_PER_PAGE = 10;
    private final boolean fuzzySearch;

    public EntityScreenContext(int offset, String searchText, int sortIndex, int casesPerPage,
            String[] selectedValues, String detailSelection, boolean isFuzzySearch) {
        mOffSet = offset;
        mSearchText = searchText;
        mSortIndex = sortIndex;
        mCasesPerPage = casesPerPage;
        mSelectedValues = selectedValues;
        mDetailSelection = detailSelection;
        fuzzySearch = isFuzzySearch;
    }

    public EntityScreenContext() {
        mOffSet = 0;
        mSearchText = null;
        mSortIndex = 0;
        mCasesPerPage = DEFAULT_CASES_PER_PAGE;
        mSelectedValues = null;
        mDetailSelection = null;
        fuzzySearch = false;
    }

    public int getOffSet() {
        return mOffSet;
    }

    public String getSearchText() {
        return mSearchText;
    }

    public int getSortIndex() {
        return mSortIndex;
    }

    public int getCasesPerPage() {
        return mCasesPerPage;
    }

    public String[] getSelectedValues() {
        return mSelectedValues;
    }

    public String getDetailSelection() {
        return mDetailSelection;
    }

    public boolean isFuzzySearch() {
        return fuzzySearch;
    }
}

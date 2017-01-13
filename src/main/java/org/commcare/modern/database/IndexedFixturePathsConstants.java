package org.commcare.modern.database;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixturePathsConstants {
    public final static String INDEXED_FIXTURE_INDEX_TABLE = "IndexedFixtureIndex";
    public final static String INDEXED_FIXTURE_INDEX_COL_NAME = "name";
    public final static String INDEXED_FIXTURE_INDEX_COL_BASE = "base";
    public final static String INDEXED_FIXTURE_INDEX_COL_CHILD = "child";
    public final static String INDEXED_FIXTURE_INDEX_TABLE_STMT =
            "CREATE TABLE IF NOT EXISTS " +
                    IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEX_TABLE +
                    " (" + IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEX_COL_NAME +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEX_COL_BASE +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEX_COL_CHILD + ");";

    public final static String INDEXED_FIXTURE_INDEXING_STMT =
            DatabaseIndexingUtils.indexOnTableCommand("fixture_name_index",
                    IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEX_TABLE, IndexedFixturePathsConstants.INDEXED_FIXTURE_INDEX_COL_NAME);
}

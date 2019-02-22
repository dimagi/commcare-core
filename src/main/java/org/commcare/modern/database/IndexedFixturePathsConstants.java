package org.commcare.modern.database;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixturePathsConstants {
    public final static String INDEXED_FIXTURE_PATHS_TABLE = "IndexedFixtureIndex";
    public final static String INDEXED_FIXTURE_PATHS_COL_NAME = "name";
    public final static String INDEXED_FIXTURE_PATHS_COL_BASE = "base";
    public final static String INDEXED_FIXTURE_PATHS_COL_CHILD = "child";
    public final static String INDEXED_FIXTURE_PATHS_COL_LAST_SYNC = "last_sync";
    public final static String INDEXED_FIXTURE_PATHS_TABLE_STMT =
            "CREATE TABLE IF NOT EXISTS " +
                    IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE +
                    " (" + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_NAME + " UNIQUE" +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_BASE +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_CHILD +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_LAST_SYNC + ");";

    public final static String INDEXED_FIXTURE_INDEXING_STMT =
            DatabaseIndexingUtils.indexOnTableCommand("fixture_name_index",
                    IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE, IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_NAME);

    public final static String INDEXED_FIXTURE_PATHS_TABLE_STMT_V15 =
            "CREATE TABLE IF NOT EXISTS " +
                    IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_TABLE +
                    " (" + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_NAME + " UNIQUE" +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_BASE +
                    ", " + IndexedFixturePathsConstants.INDEXED_FIXTURE_PATHS_COL_CHILD + ");";

}

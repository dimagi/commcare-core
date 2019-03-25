package org.commcare.modern.database;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixturePathsConstants {

    public final static String INDEXED_FIXTURE_PATHS_TABLE = "IndexedFixtureIndex";
    public final static String INDEXED_FIXTURE_PATHS_COL_NAME = "name";
    public final static String INDEXED_FIXTURE_PATHS_COL_BASE = "base";
    public final static String INDEXED_FIXTURE_PATHS_COL_CHILD = "child";
    public final static String INDEXED_FIXTURE_PATHS_COL_ATTRIBUTES = "attributes";

    public final static String INDEXED_FIXTURE_PATHS_TABLE_STMT =
            "CREATE TABLE IF NOT EXISTS " +
                    INDEXED_FIXTURE_PATHS_TABLE +
                    " (" + INDEXED_FIXTURE_PATHS_COL_NAME + " UNIQUE" +
                    ", " + INDEXED_FIXTURE_PATHS_COL_BASE +
                    ", " + INDEXED_FIXTURE_PATHS_COL_CHILD +
                    ", " + INDEXED_FIXTURE_PATHS_COL_ATTRIBUTES + ");";

    public final static String INDEXED_FIXTURE_INDEXING_STMT =
            DatabaseIndexingUtils.indexOnTableCommand("fixture_name_index",
                    INDEXED_FIXTURE_PATHS_TABLE, INDEXED_FIXTURE_PATHS_COL_NAME);

    public final static String INDEXED_FIXTURE_PATHS_TABLE_STMT_V15 =
            "CREATE TABLE IF NOT EXISTS " +
                    INDEXED_FIXTURE_PATHS_TABLE +
                    " (" + INDEXED_FIXTURE_PATHS_COL_NAME + " UNIQUE" +
                    ", " + INDEXED_FIXTURE_PATHS_COL_BASE +
                    ", " + INDEXED_FIXTURE_PATHS_COL_CHILD + ");";
}

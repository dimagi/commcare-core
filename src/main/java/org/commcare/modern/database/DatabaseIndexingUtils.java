package org.commcare.modern.database;

import java.util.Set;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class DatabaseIndexingUtils {

    /**
     * Build SQL command to create an index on a table
     *
     * @param indexName        Name of index on the table
     * @param tableName        Table target of index being created
     * @param columnListString One or more columns used to create the index.
     *                         Multiple columns should be comma-seperated.
     * @return Indexed table creation SQL command.
     */
    public static String indexOnTableCommand(String indexName,
                                             String tableName,
                                             String columnListString) {
        return "CREATE INDEX " + indexName + " ON " +
                tableName + "( " + columnListString + " )";
    }

    public static String[] getIndexStatements(String tableName, Set<String> indices) {
        String[] indexStatements = new String[indices.size()];
        int i = 0;
        for (String index : indices) {
            indexStatements[i++] = makeIndexingStatement(tableName, index);
        }
        return indexStatements;
    }

    private static String makeIndexingStatement(String tableName, String index) {
        String indexName = index + "_index";
        if (index.contains(",")) {
            indexName = index.replaceAll(",", "_") + "_index";
        }
        return indexOnTableCommand(indexName, tableName, index);
    }
}

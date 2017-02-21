package org.commcare.api.persistence;

import org.commcare.modern.util.Pair;
import org.commcare.modern.database.*;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set of Sql utility methods for clients running modern, non-Android Java (where prepared
 * statements in the place of cursors)
 * <p/>
 * All methods that return a ResultSet expect a PreparedStatement as an argument and the caller
 * is responsible for closing this statement when its finished with it.
 * <p/>
 * Created by wpride1 on 8/11/15.
 */
public class SqlHelper {

    public static final boolean SQL_DEBUG = true;

    public static void dropTable(Connection c, String storageKey) {
        String sqlStatement = "DROP TABLE IF EXISTS " + storageKey;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(sqlStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            Logger.log("E", "Could not drop table: " + e.getMessage());
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void createTable(Connection c, String storageKey, Persistable p) {
        String sqlStatement = DatabaseHelper.getTableCreateString(storageKey, p);
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(sqlStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static PreparedStatement prepareIdSelectStatement(Connection c, String storageKey, int id) {
        try {
            PreparedStatement preparedStatement =
                    c.prepareStatement("SELECT * FROM " + storageKey + " WHERE "
                            + DatabaseHelper.ID_COL + " = ?;");
            preparedStatement.setInt(1, id);
            return preparedStatement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws IllegalArgumentException when one or more of the fields we're selecting on
     *                                  is not a valid key to select on for this object
     */
    public static PreparedStatement prepareTableSelectStatement(Connection c,
                                                                String storageKey,
                                                                String[] fields,
                                                                String[] values) {
        Pair<String, String[]> pair = DatabaseHelper.createWhere(fields, values, null);
        try {
            String queryString =
                    "SELECT * FROM " + storageKey + " WHERE " + pair.first + ";";
            PreparedStatement preparedStatement = c.prepareStatement(queryString);
            for (int i = 0; i < pair.second.length; i++) {
                preparedStatement.setString(i + 1, pair.second[i]);
            }
            return preparedStatement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws IllegalArgumentException when one or more of the fields we're selecting on
     *                                  is not a valid key to select on for this object
     */
    public static PreparedStatement prepareTableSelectStatement(Connection c,
                                                                String storageKey,
                                                                String where,
                                                                String values[]) {
        try {
            String queryString =
                    "SELECT * FROM " + storageKey + " WHERE " + where + ";";
            PreparedStatement preparedStatement = c.prepareStatement(queryString);
            for (int i = 0; i < values.length; i++) {
                preparedStatement.setString(i + 1, values[i]);
            }
            return preparedStatement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void basicInsert(Connection c, String storageKey,
                                   Map<String, String> contentVals) {
        PreparedStatement preparedStatement = null;
        Pair<List<String>, String> valsAndInsertStatement =
                buildInsertStatement(storageKey, contentVals);
        try {
            preparedStatement = c.prepareStatement(valsAndInsertStatement.second);
            int i = 1;
            for (String val : valsAndInsertStatement.first) {
                preparedStatement.setString(i++, val);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Pair<List<String>, String> buildInsertStatement(String storageKey,
                                                                   Map<String, String> contentVals) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("INSERT INTO ").append(storageKey).append(" (");
        List<String> values = new ArrayList<>();
        String prefix = "";
        for (String key : contentVals.keySet()) {
            stringBuilder.append(prefix);
            prefix = ",";
            stringBuilder.append(key);
            values.add(contentVals.get(key));
        }
        stringBuilder.append(") VALUES (");
        prefix = "";
        for (int i = 0; i < values.size(); i++) {
            stringBuilder.append(prefix);
            prefix = ",";
            stringBuilder.append("?");
        }
        stringBuilder.append(");");
        return Pair.create(values, stringBuilder.toString());
    }

    public static int insertToTable(Connection c, String storageKey, Persistable p) {
        Pair<String, List<Object>> mPair = DatabaseHelper.getTableInsertData(storageKey, p);
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = c.prepareStatement(mPair.first);
            for (int i = 0; i < mPair.second.size(); i++) {
                Object obj = mPair.second.get(i);

                if (obj instanceof String) {
                    preparedStatement.setString(i + 1, (String)obj);
                } else if (obj instanceof Blob) {
                    preparedStatement.setBlob(i + 1, (Blob)obj);
                } else if (obj instanceof Integer) {
                    preparedStatement.setInt(i + 1, (Integer)obj);
                } else if (obj instanceof Long) {
                    preparedStatement.setLong(i + 1, (Long)obj);
                } else if (obj instanceof byte[]) {
                    preparedStatement.setBinaryStream(i + 1, new ByteArrayInputStream((byte[])obj), ((byte[])obj).length);
                }
            }
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    p.setID(id);
                    return id;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update SQLite DB with Persistable p
     *
     * @param c          Database Connection
     * @param storageKey name of table
     * @param p          persistable to be updated
     */
    public static void updateId(Connection c, String storageKey, Persistable p) {
        HashMap<String, Object> map = DatabaseHelper.getMetaFieldsAndValues(p);

        String[] fieldNames = map.keySet().toArray(new String[map.keySet().size()]);
        Object[] values = map.values().toArray(new Object[map.values().size()]);

        Pair<String, String[]> where = org.commcare.modern.database.DatabaseHelper.createWhere(fieldNames, values, p);

        String query = "UPDATE " + storageKey + " SET " + DatabaseHelper.DATA_COL + " = ? WHERE " + where.first + ";";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(query);
            setPreparedStatementArgs(preparedStatement, p, where.second);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update entry under id with persistable p
     *
     * @param connection  Database Connection
     * @param tableName   name of table
     * @param persistable persistable to update with
     * @param id          sql record to update
     */
    public static void updateToTable(Connection connection, String tableName, Persistable persistable, int id) {
        String queryStart = "UPDATE " + tableName + " SET " + DatabaseHelper.DATA_COL + " = ? ";
        String queryEnd = " WHERE " + DatabaseHelper.ID_COL + " = ?;";

        HashMap<String, Object> map = DatabaseHelper.getMetaFieldsAndValues(persistable);
        String[] fieldNames = map.keySet().toArray(new String[map.keySet().size()]);
        Object[] values = map.values().toArray(new Object[map.values().size()]);

        StringBuilder stringBuilder = new StringBuilder(queryStart);
        for (String fieldName : fieldNames) {
            stringBuilder.append(", ").append(fieldName).append(" = ?");
        }

        String query = stringBuilder.append(queryEnd).toString();

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            int lastArgIndex = setPreparedStatementArgs(preparedStatement, persistable, values);
            preparedStatement.setInt(lastArgIndex, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param preparedStatement the PreparedStatement to populate with arguments
     * @param persistable       the Persistable object being stored
     * @param values            the ordered values to use in the PreparedStatement (corresponding to the
     *                          '?' in the query string)
     * @return the index of the next '?' NOT populated by this helper
     * @throws SQLException
     */
    public static int setPreparedStatementArgs(PreparedStatement preparedStatement,
                                               Persistable persistable,
                                               Object[] values) throws SQLException {
        byte[] blob = org.commcare.modern.database.TableBuilder.toBlob(persistable);
        preparedStatement.setBinaryStream(1, new ByteArrayInputStream(blob), blob.length);
        // offset to 2 since 1) SQLite is 1 indexed and 2) we set the first arg above
        int i = 2;
        for (Object obj : values) {
            if (obj instanceof String) {
                preparedStatement.setString(i, (String)obj);
            } else if (obj instanceof Blob) {
                preparedStatement.setBlob(i, (Blob)obj);
            } else if (obj instanceof Integer) {
                preparedStatement.setInt(i, (Integer)obj);
            } else if (obj instanceof Long) {
                preparedStatement.setLong(i, (Long)obj);
            } else if (obj instanceof byte[]) {
                preparedStatement.setBinaryStream(i, new ByteArrayInputStream((byte[])obj), ((byte[])obj).length);
            } else if (obj == null) {
                preparedStatement.setNull(i, 0);
            }
            i++;
        }
        return i;
    }

    public static void deleteFromTableWhere(Connection connection, String tableName, String whereClause, String arg) {
        String query = "DELETE FROM " + tableName + " " + whereClause + ";";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, arg);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Update entry under id with persistable p
     *
     * @param connection Database Connection
     * @param tableName  name of table
     * @param id         sql record to update
     */
    public static void deleteIdFromTable(Connection connection, String tableName, int id) {
        String query = "DELETE FROM " + tableName + " WHERE " + DatabaseHelper.ID_COL + " = ?;";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Update entry under id with persistable p
     *
     * @param connection Database Connection
     * @param tableName  name of table
     */
    public static void deleteAllFromTable(Connection connection, String tableName) {
        String query = "DELETE FROM " + tableName;

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}

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
import java.util.HashMap;
import java.util.List;

/**
 * Set of Sql utility methods for clients running modern, non-Android Java (where we use cursors
 * in the place of prepared statements)
 *
 * Created by wpride1 on 8/11/15.
 */
public class SqlHelper {
    public static void dropTable(Connection c, String storageKey) {
        String sqlStatement = "DROP TABLE IF EXISTS " + storageKey;
        try {
            executeSql(c, sqlStatement);
        } catch (SQLException e) {
            Logger.log("E", "Could not drop table: " + e.getMessage());
        }
    }

    public static ResultSet executeSql(Connection c, String sqlQuery) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(sqlQuery);
            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally{
            if(preparedStatement != null){
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
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
            //fail silently, expected sometimes
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

    public static ResultSet selectForId(Connection c, String storageKey, int id) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement("SELECT * FROM " + storageKey + " WHERE "
                    + DatabaseHelper.ID_COL + " = ?;");
            preparedStatement.setInt(1, id);
            return preparedStatement.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
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

    public static ResultSet selectFromTable(Connection c, String storageKey, String[] fields, String[] values, Persistable p) {
        org.commcare.modern.database.TableBuilder mTableBuilder = new org.commcare.modern.database.TableBuilder(storageKey);
        mTableBuilder.addData(p);
        Pair<String, String[]> mPair = DatabaseHelper.createWhere(fields, values, p);
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement("SELECT * FROM " + storageKey + " WHERE " + mPair.first + ";");
            for (int i = 0; i < mPair.second.length; i++) {
                preparedStatement.setString(i + 1, mPair.second[i]);
            }
            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
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
                    preparedStatement.setInt(i + 1, (Integer) obj);
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
            System.out.println("e: " + e);
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;
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

        String[] fieldNames = map.keySet().toArray(new String[0]);
        Object[] values = map.values().toArray(new Object[0]);

        Pair<String, String[]> where = org.commcare.modern.database.DatabaseHelper.createWhere(fieldNames, values, p);

        String query = "UPDATE " + storageKey + " SET " + DatabaseHelper.DATA_COL + " = ? WHERE " + where.first + ";";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(query);

            byte[] blob = org.commcare.modern.database.TableBuilder.toBlob(p);

            preparedStatement.setBinaryStream(1, new ByteArrayInputStream(blob), (blob).length);
            /*
             * We have to do this weird number stuff because 1) our first arg has already been set
             * (DATA_COL above) and 2) preparedStatement arguments are 1-indexed
             */
            for (int i = 2; i < where.second.length + 2; i++) {
                Object obj = where.second[i - 2];
                if (obj instanceof String) {
                    preparedStatement.setString(i, (String)obj);
                } else if (obj instanceof Blob) {
                    preparedStatement.setBlob(i, (Blob)obj);
                } else if (obj instanceof Integer) {
                    preparedStatement.setInt(i, (Integer)obj);
                } else if (obj instanceof byte[]) {
                    preparedStatement.setBinaryStream(i, new ByteArrayInputStream((byte[])obj), ((byte[])obj).length);
                } else if (obj == null) {
                    preparedStatement.setNull(i, 0);
                }
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
     * @param connection         Database Connection
     * @param tableName name of table
     * @param persistable         persistable to update with
     * @param id        sql record to update
     */
    public static void updateToTable(Connection connection, String tableName, Persistable persistable, int id) {
        String query = "UPDATE " + tableName + " SET " + DatabaseHelper.DATA_COL + " = ? " + " WHERE " + DatabaseHelper.ID_COL + " = ?;";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);

            byte[] blob = org.commcare.modern.database.TableBuilder.toBlob(persistable);

            preparedStatement.setBinaryStream(1, new ByteArrayInputStream(blob), blob.length);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
}

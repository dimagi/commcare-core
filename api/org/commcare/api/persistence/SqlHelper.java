package org.commcare.api.persistence;

import org.commcare.api.util.Pair;
import org.commcare.core.database.DatabaseHelper;
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
 * Created by wpride1 on 8/11/15.
 */
public class SqlHelper {
    public static void dropTable(Connection c, String storageKey){
        String sqlStatement = "DROP TABLE IF EXISTS " + storageKey;
        try {
            PreparedStatement preparedStatement = c.prepareStatement(sqlStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            System.out.println("didn't drop table");
            //e.printStackTrace();
        }
    }

    public static ResultSet executeSql(Connection c, String sqlQuery){
        try {
            PreparedStatement preparedStatement = c.prepareStatement(sqlQuery);
            ResultSet rs = preparedStatement.executeQuery();
            return rs;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getRecordForValue(Connection c, String storageKey, String id){
        String sqlStatement = DatabaseHelper.getRecordForValueSelectString(storageKey, id);
        try {
            PreparedStatement preparedStatement = c.prepareStatement(sqlStatement);
            ResultSet rs = preparedStatement.executeQuery();
            byte[] bytes = rs.getBytes(1);
            return bytes;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createTable(Connection c, String storageKey, Persistable p){
        String sqlStatement = DatabaseHelper.getTableCreateString(storageKey, p);
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(sqlStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            System.out.println("Caught create table exception: " + e);
        } finally{
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch(SQLException e){

                }
            }
        }
    }

    public static ResultSet selectForId(Connection c, String storageKey, int id){
        try {
            PreparedStatement preparedStatement = c.prepareStatement("SELECT * FROM " + storageKey + " WHERE "
                    + DatabaseHelper.ID_COL + " = ?;");
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            return rs;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ResultSet selectFromTable(Connection c, String storageKey, String[] fields, String[]values, Persistable p){
        TableBuilder mTableBuilder = new TableBuilder(storageKey);
        mTableBuilder.addData(p);
        Pair<String, String[]> mPair = DatabaseHelper.createWhere(fields, values, p);

        try {
            PreparedStatement preparedStatement = c.prepareStatement("SELECT * FROM " + storageKey + " WHERE " + mPair.first +";");
            for(int i=0; i<mPair.second.length; i++){
                preparedStatement.setString(i+1, mPair.second[i]);
            }
            ResultSet rs = preparedStatement.executeQuery();
            return rs;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int insertToTable(Connection c, String storageKey, Persistable p){

        Pair<String, List<Object>> mPair = DatabaseHelper.getTableInsertData(storageKey, p);

        try {
            PreparedStatement preparedStatement = c.prepareStatement(mPair.first);
            for(int i=0; i<mPair.second.size(); i++){
                Object obj = mPair.second.get(i);

                if(obj instanceof String){
                    preparedStatement.setString(i + 1, (String) obj);
                } else if(obj instanceof Blob){
                    preparedStatement.setBlob(i+1, (Blob) obj);
                } else if(obj instanceof Integer){
                    preparedStatement.setInt(i + 1, ((Integer) obj).intValue());
                } else if(obj instanceof byte[]){
                    preparedStatement.setBinaryStream(i+1,new ByteArrayInputStream((byte[]) obj), ((byte[]) obj).length);
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
                }
                else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            System.out.println("e: " + e);
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Update SQLite DB with Persistable p
     * @param c Database Connection
     * @param storageKey name of table
     * @param p peristable to be updated
     */

    public static void updateId(Connection c, String storageKey, Persistable p) {

        HashMap<String, Object> map = DatabaseHelper.getMetaFieldsAndValues(p);

        String[] fieldnames = map.keySet().toArray(new String[0]);
        Object[] values = map.values().toArray(new Object[0]);

        Pair<String, String[]> where = org.commcare.core.database.DatabaseHelper.createWhere(fieldnames, values, p);

        String query = "UPDATE " + storageKey + " SET " + DatabaseHelper.DATA_COL + " = ? WHERE " + where.first + ";";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(query);

            byte[] blob = TableBuilder.toBlob(p);

            preparedStatement.setBinaryStream(1, new ByteArrayInputStream((byte[]) blob), ((byte[]) blob).length);
            /*
             * We have to do this weird number stuff because 1) our first arg has already been set
             * (DATA_COL above) and 2) preparedStatement arguments are 1-indexed
             */
            for(int i=2; i<where.second.length + 2; i++){
                Object obj = where.second[i-2];
                if(obj instanceof String){
                    preparedStatement.setString(i, (String) obj);
                } else if(obj instanceof Blob){
                    preparedStatement.setBlob(i, (Blob) obj);
                } else if(obj instanceof Integer){
                    preparedStatement.setInt(i, ((Integer) obj).intValue());
                } else if(obj instanceof byte[]){
                    preparedStatement.setBinaryStream(i,new ByteArrayInputStream((byte[]) obj), ((byte[]) obj).length);
                } else if(obj == null) {
                    preparedStatement.setNull(i, 0);
                }
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Update entry under id with persistable p
     * @param c Database Connection
     * @param tableName name of table
     * @param p peristable to udpate with
     * @param id sql record to update
     */
    public static void updateToTable(Connection c, String tableName, Persistable p, int id) {
        String query = "UPDATE " + tableName + " SET " + DatabaseHelper.DATA_COL + " = ? " + " WHERE " + DatabaseHelper.ID_COL + " = ?;";

        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(query);

            byte[] blob = TableBuilder.toBlob(p);

            preparedStatement.setBinaryStream(1, new ByteArrayInputStream(blob), blob.length);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}

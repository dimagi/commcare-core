/**
 *
 */

package org.commcare.api.persistence;

import org.commcare.api.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ctsims
 *
 */
public class UserDatabaseHelper {

    public static String ID_COL = "commcare_sql_id";
    public static String DATA_COL = "commcare_sql_record";

    public static Pair<String, String[]> createWhere(String[] fieldNames, Object[] values, Persistable p)  throws IllegalArgumentException {
        Set<String> fields = null;
        if(p instanceof IMetaData) {
            IMetaData m = (IMetaData)p;
            String[] thefields = m.getMetaDataFields();
            fields = new HashSet<String>();
            for(String s : thefields) {
                fields.add(TableBuilder.scrubName(s));
            }
        }

        String ret = "";
        String[] arguments = new String[fieldNames.length];
        for(int i = 0 ; i < fieldNames.length; ++i) {
            String columnName = TableBuilder.scrubName(fieldNames[i]);
            if(fields != null) {
                if(!fields.contains(columnName)) {
                    throw new IllegalArgumentException("Model does not contain the column " + columnName + "!");
                }
            }
            ret += columnName + "=?";

            arguments[i] = values[i].toString();

            if(i + 1 < fieldNames.length) {
                ret += " AND ";
            }
        }
        return new Pair<String, String[]>(ret, arguments);
    }

    public static Set<String> getMetaDataFields(Persistable p){
        Set<String> fields = null;
        if(p instanceof IMetaData) {
            IMetaData m = (IMetaData)p;
            String[] thefields = m.getMetaDataFields();
            fields = new HashSet<String>();
            for(String s : thefields) {
                fields.add(TableBuilder.scrubName(s));
            }
        }
        return fields;
    }


    public static HashMap<String, Object> getContentValues(Persistable e) {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream out = bos;

        try {
            e.writeExternal(new DataOutputStream(out));
            out.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new RuntimeException("Failed to serialize externalizable for content values");
        }
        byte[] blob = bos.toByteArray();

        HashMap<String, Object> values = new HashMap<String, Object>();

        if(e instanceof IMetaData) {
            IMetaData m = (IMetaData)e;
            for(String key : m.getMetaDataFields()) {
                Object o = m.getMetaData(key);
                if(o == null ) { continue;}
                String value = o.toString();
                values.put(TableBuilder.scrubName(key), value);
            }
        }

        values.put(DATA_COL, blob);

        return values;
    }

    public static String getTableCreateString(String storageKey, Persistable p){
        TableBuilder mTableBuilder = new TableBuilder(storageKey);
        mTableBuilder.addData(p);
        return mTableBuilder.getTableCreateString();
    }

    public static String getTableInsertString(String storageKey, Persistable p){
        TableBuilder mTableBuilder = new TableBuilder(storageKey);
        mTableBuilder.addData(p);
        return mTableBuilder.getTableInsertString(p);
    }

    public static Pair<String, List<Object>> getTableInsertData(String storageKey, Persistable p){
        TableBuilder mTableBuilder = new TableBuilder(storageKey);
        mTableBuilder.addData(p);
        return mTableBuilder.getTableInsertData(p);
    }

    public static void createTable(Connection c, String storageKey, Persistable p){
        String sqlStatement = getTableCreateString(storageKey, p);
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = c.prepareStatement(sqlStatement);
            boolean createdTable = preparedStatement.execute();
        } catch (SQLException e) {
            System.out.println("SQLE: " + e);
            e.printStackTrace();
        } finally{
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch(SQLException e){

                }
            }
        }
    }

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

    public static ResultSet selectForId(Connection c, String storageKey, int id){
        try {
            System.out.println("STATEMENT: " + "SELECT * FROM " + storageKey + " WHERE "
                    + TableBuilder.ID_COL + " = ?; " + id);
            PreparedStatement preparedStatement = c.prepareStatement("SELECT * FROM " + storageKey + " WHERE "
                    + TableBuilder.ID_COL + " = ?;");
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            System.out.println("rs: " + rs.isClosed());
            return rs;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static ResultSet selectFromTable(Connection c, String storageKey, String[] fields, String[]values, Persistable p){
        TableBuilder mTableBuilder = new TableBuilder(storageKey);
        mTableBuilder.addData(p);
        Pair<String, String[]> mPair = createWhere(fields, values, p);

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

    public static void insertToTable(Connection c, String storageKey, Persistable p){

        Pair<String, List<Object>> mPair = getTableInsertData(storageKey, p);

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
                    p.setID(generatedKeys.getInt(1));
                }
                else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            System.out.println("e: " + e);
            e.printStackTrace();
        }
    }

    public static byte[] getRecordForValue(Connection c, String storageKey, String id){
        String sqlStatement = "SELECT " + DATA_COL + " from " + storageKey + " WHERE commcare_sql_id = " + id + ";";
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


    /*
    public PrototypeFactory getPrototypeFactory() {
        return DbUtil.getPrototypeFactory(c);
    }
    */
}

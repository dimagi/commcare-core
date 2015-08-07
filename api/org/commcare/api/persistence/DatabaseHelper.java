/**
 *
 */

package org.commcare.api.persistence;

import org.commcare.api.models.EncryptedModel;
import org.commcare.api.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.Externalizable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Setup of platform-agnostic DB helper functions IE for generating SQL
 * statements, args, content values, etc.
 *
 * @author wspride
 *
 */
public class DatabaseHelper {

    public static String ID_COL = "commcare_sql_id";
    public static String DATA_COL = "commcare_sql_record";

    public static Pair<String, String[]> createWhere(String[] fieldNames, Object[] values,  Persistable p)  throws IllegalArgumentException {
        return createWhere(fieldNames, values, null, p);
    }

    public static Pair<String, String[]> createWhere(String[] fieldNames, Object[] values,  EncryptedModel em, Persistable p)  throws IllegalArgumentException {
        Set<String> fields = null;
        if(p instanceof IMetaData) {
            IMetaData m = (IMetaData)p;
            String[] thefields = m.getMetaDataFields();
            fields = new HashSet<String>();
            for(String s : thefields) {
                fields.add(TableBuilder.scrubName(s));
            }
        }


        if(em instanceof IMetaData) {
            IMetaData m = (IMetaData)em;
            String[] thefields = m.getMetaDataFields();
            //fields = new HashSet<String>();
            for(String s : thefields) {
                fields.add(TableBuilder.scrubName(s));
            }
        }

        String ret = "";
        ArrayList<String> arguments = new ArrayList<String>();
        boolean set = false;
        for(int i = 0 ; i < fieldNames.length; ++i) {
            String columnName = TableBuilder.scrubName(fieldNames[i]);
            if(fields != null) {
                if(!fields.contains(columnName)) {
                    continue;
                }
            }

            if(set){
                ret += " AND ";
            }

            ret += columnName + "=?";

            arguments.add(values[i].toString());

            set = true;
        }

        String[] retArray = new String[arguments.size()];
        for(int i =0; i< arguments.size(); i++){
            retArray[i] = arguments.get(i);
        }

        return new Pair<String, String[]>(ret, retArray);
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


    public static HashMap<String, Object> getMetaFieldsAndValues(Externalizable e) {

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
            preparedStatement.execute();
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
            PreparedStatement preparedStatement = c.prepareStatement("SELECT * FROM " + storageKey + " WHERE "
                    + ID_COL + " = ?;");
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

    public static int insertToTable(Connection c, String storageKey, Persistable p){

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

    /**
     * Update SQLite DB with Persistable p
     * @param c Database Connection
     * @param storageKey name of table
     * @param p peristable to be updated
     */

    public static void updateId(Connection c, String storageKey, Persistable p) {

        HashMap<String, Object> map = getMetaFieldsAndValues(p);

        String[] fieldnames = map.keySet().toArray(new String[0]);
        Object[] values = map.values().toArray(new Object[0]);

        Pair<String, String[]> where = DatabaseHelper.createWhere(fieldnames, values, p);

        String query = "UPDATE " + storageKey + " SET " + DATA_COL + " = ? WHERE " + where.first + ";";

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
        String query = "UPDATE " + tableName + " SET " + DATA_COL + " = ? " + " WHERE " + ID_COL + " = ?;";

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

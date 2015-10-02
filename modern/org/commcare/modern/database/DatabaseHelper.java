package org.commcare.modern.database;

import org.commcare.modern.models.EncryptedModel;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.Externalizable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
            fields = new HashSet<String>();
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

    public static String getRecordForValueSelectString(String storageKey, String id){
        return "SELECT " + DatabaseHelper.DATA_COL + " from " + storageKey + " WHERE commcare_sql_id = " + id + ";";
    }


}

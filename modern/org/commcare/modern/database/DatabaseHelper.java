package org.commcare.modern.database;

import org.commcare.modern.models.EncryptedModel;
import org.commcare.modern.models.RecordTooLargeException;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.Externalizable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    public static final String ID_COL = "commcare_sql_id";
    public static final String DATA_COL = "commcare_sql_record";

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


        StringBuilder stringBuilder = new StringBuilder();
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
                stringBuilder.append(" AND ");
            }

            stringBuilder.append(columnName);
            stringBuilder.append("=?");

            arguments.add(values[i].toString());

            set = true;
        }
        // we couldn't match any of the fields to our columns
        if(!set){
            throw new IllegalArgumentException();
        }

        String[] retArray = new String[arguments.size()];
        for(int i =0; i< arguments.size(); i++){
            retArray[i] = arguments.get(i);
        }

        return new Pair<String, String[]>(stringBuilder.toString(), retArray);
    }

    public static HashMap<String, Object> getMetaFieldsAndValues(Externalizable e) throws RecordTooLargeException{

        byte[] blob = TableBuilder.toBlob(e);

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
        if(blob.length > 1000000){
            throw new RecordTooLargeException(blob.length / 1000000);
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
}

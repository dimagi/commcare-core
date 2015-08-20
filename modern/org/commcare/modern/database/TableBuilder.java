package org.commcare.modern.database;

import org.commcare.core.models.MetaField;
import org.commcare.core.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * Functions for generating CommCare SQL statements based on classes
 *
 * Largely taken from renamed AndroidTableBuilder and moved into api to be used
 * externally.
 *
 * @author ctsims
 * @author wspride
 */
public class TableBuilder {

    private String name;

    private Vector<String> cols;
    private Vector<String> rawCols;

    public TableBuilder(Class c, String name) {
        this.name = name;
        cols = new Vector<String>();
        rawCols = new Vector<String>();
        this.addData(c);
    }


    public TableBuilder(Class c) {
        this.name = c.getSimpleName();
        cols = new Vector<String>();
        rawCols = new Vector<String>();
        addData(c);
    }

    public TableBuilder(String name) {
        this.name = name;
        cols = new Vector<String>();
        rawCols = new Vector<String>();
    }

    public void addData(Class c) {
        cols.add(org.commcare.modern.database.DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY");
        rawCols.add(org.commcare.modern.database.DatabaseHelper.ID_COL);

        for(Field f : c.getDeclaredFields()) {
            if(f.isAnnotationPresent(MetaField.class)) {
                MetaField mf = f.getAnnotation(MetaField.class);
                addMetaField(mf);
            }
        }

        for(Method m : c.getDeclaredMethods()) {
            if(m.isAnnotationPresent(MetaField.class)) {
                MetaField mf = m.getAnnotation(MetaField.class);
                addMetaField(mf);
            }
        }

        cols.add(org.commcare.modern.database.DatabaseHelper.DATA_COL + " BLOB");
        rawCols.add(org.commcare.modern.database.DatabaseHelper.DATA_COL);
    }


    protected void addMetaField(MetaField mf) {
        String key = mf.value();
        String columnName = scrubName(key);
        rawCols.add(columnName);
        String columnDef;
        columnDef = columnName;

        //Modifiers
        if(unique.contains(columnName) || mf.unique()) {
            columnDef += " UNIQUE";
        }
        cols.add(columnDef);
    }

    public void addData(Persistable p) {
        cols.add(org.commcare.modern.database.DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY");
        rawCols.add(org.commcare.modern.database.DatabaseHelper.ID_COL);

        if(p instanceof IMetaData) {
            String[] keys = ((IMetaData)p).getMetaDataFields();
            for(String key : keys) {
                String columnName = scrubName(key);
                if(!rawCols.contains(columnName)) {
                    rawCols.add(columnName);
                    String columnDef = columnName;

                    //Modifiers
                    if (unique.contains(columnName)) {
                        columnDef += " UNIQUE";
                    }
                    cols.add(columnDef);
                }
            }
        }

        cols.add(org.commcare.modern.database.DatabaseHelper.DATA_COL + " BLOB");
        rawCols.add(org.commcare.modern.database.DatabaseHelper.DATA_COL);
    }


    HashSet<String> unique = new HashSet<String>();
    public void setUnique(String columnName) {
        unique.add(scrubName(columnName));
    }

    public String getTableCreateString() {

        String built = "CREATE TABLE " + scrubName(name) + " (";
        for(int i = 0 ; i < cols.size() ; ++i) {
            built += cols.elementAt(i);
            if(i < cols.size() - 1) {
                built += ", ";
            }
        }
        built += ");";
        return built;
    }

    public Pair<String, List<Object>> getTableInsertData(Persistable p){
        String built = "INSERT INTO " + scrubName(name) + " (";
        HashMap<String, Object> contentValues = org.commcare.modern.database.DatabaseHelper.getMetaFieldsAndValues(p);

        ArrayList<Object> params = new ArrayList<Object>();


        for(int i = 0 ; i < rawCols.size() ; ++i) {
            built += rawCols.elementAt(i);
            if(i < rawCols.size() - 1) {
                built += ", ";
            }
        }

        built += ") VALUES (";

        for(int i = 0 ; i < rawCols.size() ; ++i) {
            Object currentValue = contentValues.get(rawCols.elementAt(i));
            built += "?";
            params.add(currentValue);
            if(i < rawCols.size() - 1) {
                built += ", ";
            }
        }

        built += ");";

        return new Pair(built, params);
    }

    //sqlite doesn't like dashes
    public static String scrubName(String input) {
        return input.replace("-", "_");
    }

    public static byte[] toBlob(Persistable p){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream out = bos;
        try {
            p.writeExternal(new DataOutputStream(out));
            out.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new RuntimeException("Failed to serialize externalizable for content values");
        }
        return bos.toByteArray();
    }

    public String getColumns() {
        String columns = "";
        for(int i = 0 ; i < rawCols.size() ; ++i) {
            columns += rawCols.elementAt(i);
            if(i < rawCols.size() - 1) {
                columns += ",";
            }
        }
        return columns;
    }
}

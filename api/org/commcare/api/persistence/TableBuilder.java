package org.commcare.api.persistence;

import org.commcare.api.util.MetaField;
import org.commcare.api.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author ctsims
 *
 */
public class TableBuilder {

    private String name;
    private Class c;

    private Vector<String> cols;
    private Vector<String> rawCols;

    public static String ID_COL = "commcare_sql_id";
    public static String DATA_COL = "commcare_sql_record";

    public TableBuilder(Class c) {
        this.c = c;
        this.name = "name";

        cols = new Vector<String>();
        rawCols = new Vector<String>();

        addData(c);
    }
    public void addData(Class c) {
        cols.add(ID_COL + " INTEGER PRIMARY KEY");
        rawCols.add(ID_COL);

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

        cols.add(DATA_COL + " BLOB");
        rawCols.add(DATA_COL);
    }


    private void addMetaField(MetaField mf) {
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

    //Option Two - For models not made natively
    public TableBuilder(String name) {
        this.name = name;
        cols = new Vector<String>();
        rawCols = new Vector<String>();
    }

    public void addData(Persistable p) {
        cols.add(ID_COL + " INTEGER PRIMARY KEY");
        rawCols.add(ID_COL);

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

        cols.add(DATA_COL + " BLOB");
        rawCols.add(DATA_COL);
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

    public String getTableInsertString(Persistable p){
        String built = "INSERT INTO " + scrubName(name) + " (";
        HashMap<String, Object> contentValues = UserDatabaseHelper.getContentValues(p);


        for(int i = 0 ; i < rawCols.size() ; ++i) {
            built += rawCols.elementAt(i);
            if(i < rawCols.size() - 1) {
                built += ", ";
            }
        }

        byte[] blob = toBlob(p);

        built += ") VALUES (";
        for(int i = 0 ; i < rawCols.size() ; ++i) {
            Object currentValue = contentValues.get(rawCols.elementAt(i));
            if(currentValue instanceof String){
                built += "`" + contentValues.get(rawCols.elementAt(i)) + "`";
            } else {
                built += contentValues.get(rawCols.elementAt(i));
            }
            if(i < rawCols.size() - 1) {
                built += ", ";
            }
        }
        built += ");";

        return built;
    }


    public Pair<String, List<Object>> getTableInsertData(Persistable p){
        String built = "INSERT INTO " + scrubName(name) + " (";
        HashMap<String, Object> contentValues = UserDatabaseHelper.getContentValues(p);

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

    public static String scrubName(String input) {
        //Scrub
        return input.replace("-", "_");
    }

    //TODO: Read this from SQL, not assume from context
    private static final int MAX_SQL_ARGS = 950;

    public static List<Pair<String, String[]>> sqlList(List<Integer> input) {
        return sqlList(input, MAX_SQL_ARGS);
    }

    public static List<Pair<String, String[]>> sqlList(List<Integer> input, int maxArgs) {

        List<Pair<String, String[]>> ops = new ArrayList<Pair<String, String[]>>();

        //figure out how many iterations we'll need
        int numIterations = (int)Math.ceil(((double)input.size()) / maxArgs);

        for(int currentRound = 0 ; currentRound < numIterations ; ++currentRound) {

            int startPoint = currentRound * maxArgs;
            int lastIndex = Math.min((currentRound + 1) * maxArgs, input.size());

            String ret = "(";
            for(int i = startPoint ; i < lastIndex ; ++i) {
                ret += "?" + ",";
            }

            String[] array = new String[lastIndex - startPoint];
            int count = 0 ;
            for(int i = startPoint ; i < lastIndex ; ++i) {
                array[count++] = String.valueOf(input.get(i));
            }

            ops.add(new Pair<String, String[]>(ret.substring(0, ret.length()-1) + ")", array));

        }
        return ops;
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

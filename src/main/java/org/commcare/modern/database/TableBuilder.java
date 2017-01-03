package org.commcare.modern.database;

import org.commcare.modern.models.MetaField;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.Externalizable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    private final String name;

    private final Vector<String> cols;
    private final Vector<String> rawCols;

    public TableBuilder(Class c, String name) {
        this.name = name;
        cols = new Vector<>();
        rawCols = new Vector<>();
        this.addData(c);
    }

    public TableBuilder(String name) {
        this.name = name;
        cols = new Vector<>();
        rawCols = new Vector<>();
    }

    public void addData(Class c) {
        cols.add(DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY");
        rawCols.add(DatabaseHelper.ID_COL);

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

        cols.add(DatabaseHelper.DATA_COL + " BLOB");
        rawCols.add(DatabaseHelper.DATA_COL);
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
        addPersistableIdAndMeta(p);

        cols.add(DatabaseHelper.DATA_COL + " BLOB");
        rawCols.add(DatabaseHelper.DATA_COL);
    }

    private void addPersistableIdAndMeta(Persistable p) {
        cols.add(DatabaseHelper.ID_COL + " INTEGER PRIMARY KEY");
        rawCols.add(DatabaseHelper.ID_COL);

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
    }

    /**
     * Build a table to store provided persistable in the filesystem.  Creates
     * filepath and encrypting key columns, along with normal metadata columns
     * from the persistable
     */
    public void addFileBackedData(Persistable p) {
        addData(p);

        cols.add(DatabaseHelper.AES_COL + " BLOB");
        rawCols.add(DatabaseHelper.AES_COL);

        cols.add(DatabaseHelper.FILE_COL);
        rawCols.add(DatabaseHelper.FILE_COL);
    }

    final HashSet<String> unique = new HashSet<>();
    public void setUnique(String columnName) {
        unique.add(scrubName(columnName));
    }

    public String getTableCreateString() {

        String built = "CREATE TABLE IF NOT EXISTS " + scrubName(name) + " (";
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

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("INSERT INTO ").append(scrubName(name)).append(" (");
        HashMap<String, Object> contentValues = DatabaseHelper.getMetaFieldsAndValues(p);

        ArrayList<Object> params = new ArrayList<>();


        for(int i = 0 ; i < rawCols.size() ; ++i) {
            stringBuilder.append(rawCols.elementAt(i));
            if(i < rawCols.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(") VALUES (");

        for(int i = 0 ; i < rawCols.size() ; ++i) {
            Object currentValue = contentValues.get(rawCols.elementAt(i));
            stringBuilder.append("?");
            params.add(currentValue);
            if(i < rawCols.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(");");

        return new Pair<String, List<Object>>(stringBuilder.toString(), params);
    }

    public static String scrubName(String input) {
        // sqlite doesn't like dashes
        return input.replace("-", "_");
    }

    public static String cleanTableName(String name) {
        return name.replace(":", "_").replace(".", "_").replace("-", "_");
    }

    public static byte[] toBlob(Externalizable externalizable){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            externalizable.writeExternal(new DataOutputStream(bos));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize externalizable " + externalizable +
                " for content values wth exception " + e);
        }
        return bos.toByteArray();
    }
}

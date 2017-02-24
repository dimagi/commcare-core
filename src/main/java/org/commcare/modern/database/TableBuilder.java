package org.commcare.modern.database;

import com.carrotsearch.hppc.IntCollection;
import com.carrotsearch.hppc.cursors.IntCursor;
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
import java.util.*;

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

    private static final int MAX_SQL_ARGS = 950;

    private final Vector<String> cols;
    private final Vector<String> rawCols;
    final HashSet<String> unique = new HashSet<>();

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

        for (Field f : c.getDeclaredFields()) {
            if (f.isAnnotationPresent(MetaField.class)) {
                MetaField mf = f.getAnnotation(MetaField.class);
                addMetaField(mf);
            }
        }

        for (Method m : c.getDeclaredMethods()) {
            if (m.isAnnotationPresent(MetaField.class)) {
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
        if (unique.contains(columnName) || mf.unique()) {
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

        if (p instanceof IMetaData) {
            String[] keys = ((IMetaData)p).getMetaDataFields();
            if (keys != null) {
                for (String key : keys) {
                    String columnName = scrubName(key);
                    if (!rawCols.contains(columnName)) {
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

    public void setUnique(String columnName) {
        unique.add(scrubName(columnName));
    }

    public String getTableCreateString() {
        String built = "CREATE TABLE IF NOT EXISTS " + scrubName(name) + " (";
        for (int i = 0; i < cols.size(); ++i) {
            built += cols.elementAt(i);
            if (i < cols.size() - 1) {
                built += ", ";
            }
        }
        built += ");";
        return built;
    }

    public Pair<String, List<Object>> getTableInsertData(Persistable p) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("INSERT INTO ").append(scrubName(name)).append(" (");
        HashMap<String, Object> contentValues = DatabaseHelper.getMetaFieldsAndValues(p);

        ArrayList<Object> params = new ArrayList<>();

        for (int i = 0; i < rawCols.size(); ++i) {
            stringBuilder.append(rawCols.elementAt(i));
            if (i < rawCols.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(") VALUES (");

        for (int i = 0; i < rawCols.size(); ++i) {
            Object currentValue = contentValues.get(rawCols.elementAt(i));
            stringBuilder.append("?");
            params.add(currentValue);
            if (i < rawCols.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(");");

        return new Pair<String, List<Object>>(stringBuilder.toString(), params);
    }

    public static String scrubName(String input) {
        return input.replace("-", "_").replace(".", "_");
    }

    public static byte[] toBlob(Externalizable externalizable) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            externalizable.writeExternal(new DataOutputStream(bos));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize externalizable " + externalizable +
                    " for content values wth exception " + e);
        }
        return bos.toByteArray();
    }

    // TODO: Copied from AndroidTableBuilder, delete that when merged
    /**
     * Given a list of integer params to insert and a maximum number of args, return the
     * String containing (?, ?,...) to be used in the SQL query and the array of args
     * to replace them with
     */
    public static List<Pair<String, String[]>> sqlList(Collection<Integer> input) {
        return sqlList(input, "?");
    }

    public static List<Pair<String, String[]>> sqlList(Collection<Integer> input, String questionMarkType) {
        return sqlList(input, MAX_SQL_ARGS, questionMarkType);
    }

    private static List<Pair<String, String[]>> sqlList(Collection<Integer> input, int maxArgs, String questionMark) {

        List<Pair<String, String[]>> ops = new ArrayList<>();

        //figure out how many iterations we'll need
        int numIterations = (int)Math.ceil(((double)input.size()) / maxArgs);

        Iterator<Integer> iterator = input.iterator();

        for (int currentRound = 0; currentRound < numIterations; ++currentRound) {

            int startPoint = currentRound * maxArgs;
            int lastIndex = Math.min((currentRound + 1) * maxArgs, input.size());
            StringBuilder stringBuilder = new StringBuilder("(");
            for (int i = startPoint; i < lastIndex; ++i) {
                stringBuilder.append(questionMark);
                stringBuilder.append(",");
            }

            String[] array = new String[lastIndex - startPoint];
            int count = 0;
            for (int i = startPoint; i < lastIndex; ++i) {
                array[count++] = String.valueOf(iterator.next());
            }

            ops.add(new Pair<>(stringBuilder.toString().substring(0,
                    stringBuilder.toString().length() - 1) + ")", array));

        }
        return ops;
    }

    public static List<Pair<String, String[]>> sqlList(IntCollection input) {
        return sqlList(input, MAX_SQL_ARGS);
    }

    /**
     * Given a list of integer params to insert and a maximum number of args, return the
     * String containing (?, ?,...) to be used in the SQL query and the array of args
     * to replace them with
     */
    private static List<Pair<String, String[]>> sqlList(IntCollection input, int maxArgs) {

        List<Pair<String, String[]>> ops = new ArrayList<>();

        //figure out how many iterations we'll need
        int numIterations = (int)Math.ceil(((double)input.size()) / maxArgs);

        Iterator<IntCursor> iterator = input.iterator();

        for (int currentRound = 0; currentRound < numIterations; ++currentRound) {

            int startPoint = currentRound * maxArgs;
            int lastIndex = Math.min((currentRound + 1) * maxArgs, input.size());
            StringBuilder stringBuilder = new StringBuilder("(");
            for (int i = startPoint; i < lastIndex; ++i) {
                stringBuilder.append("?,");
            }

            String[] array = new String[lastIndex - startPoint];
            int count = 0;
            for (int i = startPoint; i < lastIndex; ++i) {
                array[count++] = String.valueOf(iterator.next().value);
            }

            ops.add(new Pair<>(stringBuilder.toString().substring(0,
                    stringBuilder.toString().length() - 1) + ")", array));

        }
        return ops;
    }
}

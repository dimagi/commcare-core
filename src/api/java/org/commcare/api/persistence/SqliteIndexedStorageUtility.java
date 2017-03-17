package org.commcare.api.persistence;

import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * IStorageIndexedUtility implemented on SQLite using JDBC. Contains all the functionality
 * for interacting with the user's SQLite representation.
 *
 * @author wspride
 */
public class SqliteIndexedStorageUtility<T extends Persistable>
        implements IStorageUtilityIndexed<T>, Iterable<T> {

    private Class<T> prototype;
    private final String tableName;
    private final String sandboxId;
    private final File databaseFolder;

    public SqliteIndexedStorageUtility(String sandboxId, String tableName,
                                       String databasePath) {
        this.tableName = tableName;
        this.sandboxId = sandboxId;
        databaseFolder = new File(databasePath);
    }

    public SqliteIndexedStorageUtility(T prototype, String sandboxId,
                                       String tableName, String databasePath) {
        this((Class<T>) prototype.getClass(), sandboxId, tableName, databasePath);
    }

    public SqliteIndexedStorageUtility(Class<T> prototype, String sandboxId,
                                       String tableName, String databasePath) {
        this(prototype, sandboxId, tableName, databasePath, true);
    }

    public SqliteIndexedStorageUtility(Class<T> prototype, String sandboxId,
                                       String tableName, String databasePath, boolean initialize) {
        this(sandboxId, tableName, databasePath);
        this.prototype = prototype;
        if (initialize) {
            try {
                buildTableFromInstance(prototype.newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void rebuildTable(T prototypeInstance) {
        this.prototype = (Class<T>) prototypeInstance.getClass();

        try {
            SqlHelper.dropTable(getConnection(), tableName);
            buildTableFromInstance(prototypeInstance);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeStatements(String[] statements) {
        Connection c = null;
        try {
            c = getConnection();
            for (String statement : statements) {
                c.prepareStatement(statement).execute();
            }
            c.close();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void basicInsert(Map<String, String> contentVals) {
        Connection c = null;
        try {
            c = getConnection();
            SqlHelper.basicInsert(c, tableName, contentVals);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildTableFromInstance(T instance) throws ClassNotFoundException {
        Connection c = null;
        try {
            c = getConnection();
            SqlHelper.createTable(c, tableName, instance);
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection() throws SQLException, ClassNotFoundException {
        if (!databaseFolder.exists()) {
            databaseFolder.mkdir();
        }
        Class.forName("org.sqlite.JDBC");
        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl("jdbc:sqlite:" + databaseFolder + "/" + this.sandboxId + ".db");
        return dataSource.getConnection();
    }

    @Override
    public void write(Persistable p) {
        if (p.getID() != -1) {
            update(p.getID(), p);
            return;
        }

        Connection c = null;
        try {
            c = getConnection();
            int id = SqlHelper.insertToTable(c, tableName, p);
            c.close();

            c = getConnection();
            p.setID(id);
            SqlHelper.updateId(c, tableName, p);
            c.close();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public T readFromBytes(byte[] mBytes) {
        T returnPrototype;
        ByteArrayInputStream mByteStream = null;
        try {
            returnPrototype = prototype.newInstance();
            mByteStream = new ByteArrayInputStream(mBytes);
            returnPrototype.readExternal(new DataInputStream(mByteStream), PrototypeManager.getDefault());
            return returnPrototype;
        } catch (InstantiationException | IllegalAccessException | DeserializationException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (mByteStream != null) {
                try {
                    mByteStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public T read(int id) {
        byte[] mBytes = readBytes(id);
        return readFromBytes(mBytes);
    }

    public static Vector<Integer> fillIdWindow(ResultSet resultSet, String columnName, LinkedHashSet newReturn) throws SQLException {
        Vector<Integer> ids = new Vector<>();
        while (resultSet.next()) {
            ids.add(resultSet.getInt(columnName));
            newReturn.add(resultSet.getInt(columnName));
        }
        return ids;
    }

    @Override
    public Vector<Integer> getIDsForValue(String fieldName, Object value) {
        Connection c = null;
        PreparedStatement preparedStatement = null;
        try {
            c = this.getConnection();
            preparedStatement = SqlHelper.prepareTableSelectStatement(c, this.tableName,
                    new String[]{fieldName}, new String[]{(String) value});
            if (preparedStatement == null) {
                return null;
            }
            ResultSet rs = preparedStatement.executeQuery();
            return fillIdWindow(rs, DatabaseHelper.ID_COL, new LinkedHashSet<Integer>());
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public T getRecordForValue(String fieldName, Object value)
            throws NoSuchElementException, InvalidIndexException {
        Connection c = null;
        PreparedStatement preparedStatement = null;
        try {
            c = this.getConnection();
            preparedStatement =
                    SqlHelper.prepareTableSelectStatement(c, this.tableName,
                            new String[]{fieldName}, new String[]{(String) value});
            ResultSet rs = preparedStatement.executeQuery();
            if (!rs.next()) {
                throw new NoSuchElementException();
            }
            byte[] mBytes = rs.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
            return readFromBytes(mBytes);
        } catch (SQLException | NullPointerException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int add(T e) {
        Connection connection = null;
        try {
            connection = getConnection();
            int id = SqlHelper.insertToTable(connection, tableName, e);
            connection.close();
            connection = getConnection();
            e.setID(id);
            SqlHelper.updateId(connection, tableName, e);
            connection.close();
            return id;
        } catch (SQLException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        // Don't need this because we close all resources after using them
    }

    @Override
    public boolean exists(int id) {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(connection, this.tableName, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public Object getAccessLock() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNumRecords() {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = getConnection();
            String sqlQuery = "SELECT COUNT (*) FROM " + this.tableName + ";";
            preparedStatement = connection.prepareStatement(sqlQuery);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.getInt(1);
        } catch (Exception e) {
            System.out.println("SqliteIndexedStorageUtility readBytes exception: " + e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    @Override
    public JdbcSqlStorageIterator<T> iterate() {
        Connection connection;
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = this.getConnection();
            String sqlQuery = "SELECT " + org.commcare.modern.database.DatabaseHelper.ID_COL + " , " +
                    org.commcare.modern.database.DatabaseHelper.DATA_COL + " FROM " + this.tableName + ";";
            preparedStatement = connection.prepareStatement(sqlQuery);
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return new JdbcSqlStorageIterator<>(preparedStatement, resultSet, this.getNumRecords(), this, connection);
    }

    @Override
    public boolean isEmpty() {
        return this.getNumRecords() <= 0;
    }

    @Override
    public byte[] readBytes(int id) {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(connection, this.tableName, id);
            if (preparedStatement == null) {
                return null;
            }
            ResultSet rs = preparedStatement.executeQuery();
            return rs.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
        } catch (Exception e) {
            System.out.println("SqliteIndexedStorageUtility readBytes exception: " + e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void update(int id, Persistable p) {
        Connection connection = null;
        try {
            connection = getConnection();
            SqlHelper.updateToTable(connection, tableName, p, id);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void remove(int id) {
        Connection connection = null;
        try {
            connection = getConnection();
            SqlHelper.deleteIdFromTable(connection, tableName, id);
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void remove(Persistable p) {
        this.remove(p.getID());
    }

    @Override
    public void removeAll() {
        Connection connection = null;
        try {
            connection = getConnection();
            SqlHelper.deleteAllFromTable(connection, tableName);
            connection.close();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // not yet implemented
    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return iterate();
    }

    public void getIDsForValues(String[] namesToMatch, String[] valuesToMatch, LinkedHashSet<Integer> ids) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = this.getConnection();
            preparedStatement = SqlHelper.prepareTableSelectStatement(connection, this.tableName,
                    namesToMatch, valuesToMatch);
            if (preparedStatement == null) {
                return;
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                ids.add(resultSet.getInt(DatabaseHelper.ID_COL));
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private RuntimeException logAndWrap(Exception e, String message) {
        RuntimeException re = new RuntimeException(message + " while inflating type " + prototype.getName());
        re.initCause(e);
        Logger.log("Error:", e.getMessage());
        return re;
    }

    /**
     * @param dbEntryId Set the deserialized persistable's id to the database entry id.
     *                  Doing so now is more effecient then during writes
     */
    public T newObject(InputStream serializedObjectInputStream, int dbEntryId) {
        try {
            T e = prototype.newInstance();
            e.readExternal(new DataInputStream(serializedObjectInputStream),
                    PrototypeManager.getDefault());
            e.setID(dbEntryId);

            return e;
        } catch (IllegalAccessException e) {
            throw logAndWrap(e, "Illegal Access Exception");
        } catch (InstantiationException e) {
            throw logAndWrap(e, "Instantiation Exception");
        } catch (IOException e) {
            throw logAndWrap(e, "Totally non-sensical IO Exception");
        } catch (DeserializationException e) {
            throw logAndWrap(e, "CommCare ran into an issue deserializing data");
        }
    }

    /**
     * @param dbEntryId Set the deserialized persistable's id to the database entry id.
     *                  Doing so now is more effecient then during writes
     */
    public T newObject(byte[] serializedObjectAsBytes, int dbEntryId) {
        return newObject(new ByteArrayInputStream(serializedObjectAsBytes), dbEntryId);
    }

    public void bulkRead(LinkedHashSet<Integer> body, HashMap<Integer, T> recordMap) {
        List<Pair<String, String[]>> whereParamList = TableBuilder.sqlList(body);
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = getConnection();
            for (Pair<String, String[]> querySet : whereParamList) {

                preparedStatement =
                        SqlHelper.prepareTableSelectStatement(connection, this.tableName,
                                DatabaseHelper.ID_COL + " IN " + querySet.first, querySet.second);
                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    int index = resultSet.findColumn(DatabaseHelper.DATA_COL);
                    byte[] data = resultSet.getBytes(index);
                    recordMap.put(resultSet.getInt(DatabaseHelper.ID_COL),
                            newObject(data, resultSet.getInt(DatabaseHelper.ID_COL)));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

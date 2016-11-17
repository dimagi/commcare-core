/**
 *
 */
package org.commcare.api.persistence;

import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * IStorageIndexedUtility implemented on SQLite using JDBC. Contains all the functionality
 * for interacting with the user's SQLite representation.
 *
 * @author wspride
 */
public class SqliteIndexedStorageUtility<T extends Persistable> implements IStorageUtilityIndexed<T>, Iterable<T> {

    private final Class<T> prototype;
    private final String tableName;
    private final String sandboxId;
    private final File databaseFolder;

    public SqliteIndexedStorageUtility(Class<T> prototype, String sandboxId, String tableName, String databasePath) {
        this.tableName = tableName;
        this.sandboxId = sandboxId;
        this.prototype = prototype;
        databaseFolder = new File(databasePath);

        Connection c = null;
        try {
            c = getConnection();
            SqlHelper.createTable(c, tableName, prototype.newInstance());
            c.prepareStatement("PRAGMA journal_mode=WAL;").execute();
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
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

    Connection getConnection() throws SQLException, ClassNotFoundException {

        if (!databaseFolder.exists()) {
            databaseFolder.mkdir();
        }

        Class.forName("org.sqlite.JDBC");
        SQLiteConnectionPoolDataSource dataSource = new SQLiteConnectionPoolDataSource();
        dataSource.setUrl("jdbc:sqlite:" + databaseFolder + "/" + this.sandboxId + ".db");
        return dataSource.getConnection();
    }

    @Override
    public void write(Persistable p) throws StorageFullException {
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

    @Override
    public Vector getIDsForValue(String fieldName, Object value) {
        Connection c = null;
        PreparedStatement preparedStatement = null;
        try {
            c = this.getConnection();
            preparedStatement = SqlHelper.prepareTableSelectStatement(c, this.tableName,
                    new String[]{fieldName}, new String[]{(String) value}, prototype.newInstance());
            if (preparedStatement == null) {
                return null;
            }
            ResultSet rs = preparedStatement.executeQuery();
            Vector<Integer> ids = new Vector<>();
            while (rs.next()) {
                ids.add(rs.getInt(org.commcare.modern.database.DatabaseHelper.ID_COL));
            }
            return ids;
        } catch (InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException e) {
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
                            new String[]{fieldName}, new String[]{(String) value},
                            prototype.newInstance());
            ResultSet rs = preparedStatement.executeQuery();
            if (!rs.next()) {
                throw new NoSuchElementException();
            }
            byte[] mBytes = rs.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
            return readFromBytes(mBytes);
        } catch (SQLException | InstantiationException |
                IllegalAccessException | NullPointerException | ClassNotFoundException e) {
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
    public int add(T e) throws StorageFullException {
        Connection c = null;
        try {
            c = getConnection();
            int id = SqlHelper.insertToTable(c, tableName, e);
            c.close();
            c = getConnection();
            e.setID(id);
            SqlHelper.updateId(c, tableName, e);
            c.close();
            return id;
        } catch (SQLException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (c != null) {
                    c.close();
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
        Connection c = null;
        try {
            c = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(c, this.tableName, id);
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
                if (c != null) {
                    c.close();
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
        Connection c = null;
        try {
            c = getConnection();
            String sqlQuery = "SELECT COUNT (*) FROM " + this.tableName + ";";
            preparedStatement = c.prepareStatement(sqlQuery);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.getInt(1);
        } catch (Exception e) {
            System.out.println("SqliteIndexedStorageUtility readBytes exception: " + e);
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
        Connection c = null;
        try {
            c = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(c, this.tableName, id);
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
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void update(int id, Persistable p) throws StorageFullException {
        Connection c = null;
        try {
            c = getConnection();
            SqlHelper.updateToTable(c, tableName, p, id);
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

    @Override
    public void remove(int id) {
        Connection c = null;
        try {
            c = getConnection();
            SqlHelper.deleteIdFromTable(c, tableName, id);
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

    @Override
    public void remove(Persistable p) {
        this.remove(p.getID());
    }

    @Override
    public void removeAll() {
        Connection c = null;
        try {
            c = getConnection();
            SqlHelper.deleteAllFromTable(c, tableName);
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

    // not yet implemented
    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        return null;
    }

    @Override
    public void registerIndex(String filterIndex) {
        // TODO Auto-generated method stub
    }

    @Override
    public Iterator<T> iterator() {
        return iterate();
    }

}

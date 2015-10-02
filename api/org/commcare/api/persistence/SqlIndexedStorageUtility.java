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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
public class SqlIndexedStorageUtility<T extends Persistable> implements IStorageUtilityIndexed<T>, Iterable<T> {

    private final Class<T> prototype;
    private final String tableName;
    private final String userName;

    public SqlIndexedStorageUtility(Class<T> prototype, String userName, String tableName) {
        this.tableName = tableName;
        this.userName = userName;
        this.prototype = prototype;
        tryCreateTable();
    }

    Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + this.userName + ".db");
        } catch (Exception e) {
            System.out.println("couldn't get jdbc sqlite driver");
        }
        return null;
    }

    /* (non-Javadoc)
    * @see org.javarosa.core.services.storage.IStorageUtility#write(org.javarosa.core.services.storage.Persistable)
    */
    public void write(Persistable p) throws StorageFullException {
        if (p.getID() != -1) {
            update(p.getID(), p);
            return;
        }

        Connection c;
        try {
            c = getConnection();
            int id = SqlHelper.insertToTable(c, tableName, p);
            c.close();

            c = getConnection();
            p.setID(id);
            SqlHelper.updateId(c, tableName, p);
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public T readFromBytes(byte[] mBytes) {

        T t;
        try {
            t = prototype.newInstance();
            ByteArrayInputStream mByteStream = new ByteArrayInputStream(mBytes);
            t.readExternal(new DataInputStream(mByteStream), PrototypeManager.getDefault());
            return t;
        } catch (InstantiationException | IllegalAccessException | DeserializationException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /* (non-Javadoc)
    * @see org.javarosa.core.services.storage.IStorageUtility#read(int)
    */
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
                    new String[]{fieldName}, new String[]{(String)value}, prototype.newInstance());
            if (preparedStatement == null) {
                return null;
            }
            ResultSet rs = preparedStatement.executeQuery();
            if(rs == null){
                return null;
            }
            Vector<Integer> ids = new Vector<>();
            while (rs.next()) {
                ids.add(rs.getInt(org.commcare.modern.database.DatabaseHelper.ID_COL));
            }
            return ids;
        } catch (InstantiationException | IllegalAccessException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        return null;
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
                            new String[]{fieldName}, new String[]{(String)value},
                            prototype.newInstance());
            if (preparedStatement == null) {
                throw new NoSuchElementException();
            }
            ResultSet rs = preparedStatement.executeQuery();
            if (rs == null || !rs.next()) {
                throw new NoSuchElementException();
            }
            byte[] mBytes = rs.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
            return readFromBytes(mBytes);
        } catch (SQLException | InstantiationException |
                IllegalAccessException | NullPointerException e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#add(org.javarosa.core.util.externalizable.Externalizable)
     */
    public int add(T e) throws StorageFullException {
        this.write(e);
        return 1;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#close()
     */
    public void close() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#destroy()
     */
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#exists(int)
     */
    public boolean exists(int id) {
        PreparedStatement preparedStatement = null;
        Connection c = null;
        try {
            c = getConnection();
            preparedStatement = SqlHelper.prepareIdSelectStatement(c, this.tableName, id);
            if (preparedStatement == null) {
                return false;
            }
            ResultSet rs = preparedStatement.executeQuery();
            if (rs != null && rs.next()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("SqlIndexedStorageUtility readBytes exception: " + e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getAccessLock()
     */
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
            System.out.println("SqlIndexedStorageUtility readBytes exception: " + e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getRecordSize(int)
     */
    public int getRecordSize(int id) {
        //serialize and test blah blah.
        return 0;
    }

    @Override
    public SqlStorageIterator<T> iterate() {
        Connection connection;
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = this.getConnection();
            String sqlQuery = "SELECT " + org.commcare.modern.database.DatabaseHelper.ID_COL + " , " +
                    org.commcare.modern.database.DatabaseHelper.DATA_COL + " FROM " + this.tableName + ";";
            preparedStatement = connection.prepareStatement(sqlQuery);
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new SqlStorageIterator<T>(preparedStatement, resultSet, this.getNumRecords(), this);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getTotalSize()
     */
    public int getTotalSize() {
        //serialize and test blah blah.
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#isEmpty()
     */
    public boolean isEmpty() {
        return this.getNumRecords() > 0;
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
            if (rs == null){
                return null;
            }
            return rs.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
        } catch (Exception e) {
            System.out.println("SqlIndexedStorageUtility readBytes exception: " + e);
        } finally{
            try {
                if (c != null) {
                    c.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#remove(int)
     */
    public void remove(int id) {

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#remove(org.javarosa.core.services.storage.Persistable)
     */
    public void remove(Persistable p) {
        this.read(p.getID());
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#removeAll()
     */
    public void removeAll() {

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#removeAll(org.javarosa.core.services.storage.EntityFilter)
     */
    public Vector<Integer> removeAll(EntityFilter ef) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#repack()
     */
    public void repack() {
        //Unecessary!
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#repair()
     */
    public void repair() {
        //Unecessary!
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#update(int, org.javarosa.core.util.externalizable.Externalizable)
     */
    public void update(int id, Persistable p) throws StorageFullException {
        Connection c;
        try {
            c = getConnection();
            SqlHelper.updateToTable(c, tableName, p, id);
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setReadOnly() {
        //TODO: This should have a clear contract.
    }

    private final Vector<String> dynamicIndices = new Vector<>();

    public void registerIndex(String filterIndex) {
        dynamicIndices.addElement(filterIndex);
    }

    @Override
    public Iterator<T> iterator() {
        return iterate();
    }

    private void tryCreateTable() {
        try {
            Connection c = getConnection();
            SqlHelper.createTable(c, tableName, prototype.newInstance());
            c.close();
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            System.out.println("Couldn't create table: " + tableName + " got: " + e);
            e.printStackTrace();
        }
    }
}

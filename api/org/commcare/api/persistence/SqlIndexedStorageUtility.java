/**
 *
 */
package org.commcare.api.persistence;

import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
public class SqlIndexedStorageUtility<T extends Persistable> implements IStorageUtilityIndexed<T>,Iterable<T>  {

    Class<T> prototype;

    PrototypeFactory mFactory;

    String tableName;
    String userName;

    public SqlIndexedStorageUtility(Class<T> prototype, PrototypeFactory factory, String userName, String tableName) {
        this.tableName = tableName;
        this.userName = userName;
        this.prototype = prototype;
        this.mFactory = factory;
        tryCreateTable();
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + this.userName + ".db");
        } catch(Exception e){
            System.out.println("couldn't get jdbc sqlite driver");
        }
        return null;
    }

    /* (non-Javadoc)
    * @see org.javarosa.core.services.storage.IStorageUtility#write(org.javarosa.core.services.storage.Persistable)
    */
    public void write(Persistable p) throws StorageFullException {
        if(p.getID() != -1) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public T readFromBytes(byte[] mBytes){

        T t = null;
        try {
            t = prototype.newInstance();
            ByteArrayInputStream mByteStream = new ByteArrayInputStream(mBytes);
            t.readExternal(new DataInputStream(mByteStream), PrototypeManager.getDefault());
            return t;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (DeserializationException e) {
            e.printStackTrace();
        } catch (IOException e) {
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

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getIDsForValue(java.lang.String, java.lang.Object)
     */
    public Vector getIDsForValue(String fieldName, Object value) {

        try {
            Connection c = this.getConnection();
            ResultSet rs = SqlHelper.selectFromTable(c, this.tableName,
                    new String[]{fieldName}, new String[]{(String) value}, prototype.newInstance());
            Vector<Integer> ids = new Vector<Integer>();
            while(rs.next()){
                ids.add(rs.getInt(org.commcare.core.database.DatabaseHelper.ID_COL));
            }
            return ids;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getRecordForValue(java.lang.String, java.lang.Object)
     */
    public T getRecordForValue(String fieldName, Object value) throws NoSuchElementException, InvalidIndexException {

        Connection c = null;
        try {
            c = this.getConnection();
            ResultSet rs = SqlHelper.selectFromTable(c, this.tableName,
                    new String[]{fieldName}, new String[]{(String) value}, prototype.newInstance());
            if(!rs.next()){
                throw new NoSuchElementException();
            }
            byte[] mBytes = rs.getBytes(org.commcare.core.database.DatabaseHelper.DATA_COL);
            c.close();
            return readFromBytes(mBytes);
        } catch (SQLException |InstantiationException | IllegalAccessException e) {
            logAndWrap(e, "Error getting record for value: " + fieldName);
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
        try {
            Connection c = getConnection();
            ResultSet rs = SqlHelper.selectForId(c, this.tableName, id);
            c.close();
            if(rs.next()){
                return true;
            }
        } catch (Exception e){
            System.out.println("SqlIndexedStorageUtility readBytes exception: " + e);
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

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getNumRecords()
     */
    public int getNumRecords() {
        try {
            Connection c = getConnection();
            ResultSet rs = SqlHelper.executeSql(c, "SELECT COUNT (*) FROM " + this.tableName + ";");
            int count = rs.getInt(1);
            c.close();
            return count;
        } catch (Exception e){
            System.out.println("SqlIndexedStorageUtility readBytes exception: " + e);
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

        Connection connection = null;
        ResultSet resultSet = null;

        try {
            connection = this.getConnection();
            resultSet = SqlHelper.executeSql(connection, "SELECT " + org.commcare.core.database.DatabaseHelper.ID_COL + " , " +
                    org.commcare.core.database.DatabaseHelper.DATA_COL + " FROM " + this.tableName + ";");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new SqlStorageIterator<T>(resultSet, this.getNumRecords(), this);
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


    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#readBytes(int)
     */
    public byte[] readBytes(int id) {
        try {
            Connection c = getConnection();
            ResultSet rs = SqlHelper.selectForId(c, this.tableName, id);
            byte[] caseBytes = rs.getBytes(org.commcare.core.database.DatabaseHelper.DATA_COL);
            c.close();
            return caseBytes;
        } catch (Exception e){
            System.out.println("SqlIndexedStorageUtility readBytes exception: " + e);
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
        Connection c = null;
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

    private RuntimeException logAndWrap(Exception e, String message) {
        RuntimeException re = new RuntimeException(message + " while inflating type " + prototype.getName());
        re.initCause(e);
        Logger.log("e", re.getMessage());
        return re;
    }

    Vector<String> dynamicIndices = new Vector<String>();

    public void registerIndex(String filterIndex) {
        dynamicIndices.addElement(filterIndex);
    }

    @Override
    public Iterator<T> iterator() {
        return iterate();
    }

    public void dropTable(){
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ this.userName + ".db");
            SqlHelper.dropTable(c, tableName);
            c.close();
        } catch(Exception e){
            System.out.println("Got exception creating table: " + tableName + " e: " + e);
            e.printStackTrace();
        }
    }

    public void tryCreateTable(){
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ this.userName + ".db");
            SqlHelper.createTable(c, tableName, prototype.newInstance());
            c.close();
        } catch(Exception e){
            System.out.println("Couldn't create table: " + tableName + " got: " + e);
        }
    }
}

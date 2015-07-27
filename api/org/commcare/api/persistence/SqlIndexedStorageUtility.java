/**
 *
 */
package org.commcare.api.persistence;

import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.DataUtil;
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
import java.util.*;

/**
 * IStorageIndexedUtility implemented on SQLite using JDBC
 *
 * @author wspride
 */
public class SqlIndexedStorageUtility<T extends Persistable> implements IStorageUtilityIndexed<T>,Iterable<T>  {

    private Hashtable<String, Hashtable<Object, Vector<Integer>>> meta;

    private Hashtable<Integer, T> data;

    int curCount;

    Class<T> prototype;

    PrototypeFactory mFactory;

    String tableName;
    String userName;

    public SqlIndexedStorageUtility(Class<T> prototype, String userName){
        this(prototype, PrototypeManager.getDefault(), userName, prototype.getName());
    }

    public SqlIndexedStorageUtility(Class<T> prototype, PrototypeFactory factory, String userName, String tableName) {
        this.tableName = tableName;
        this.userName = userName;
        this.prototype = prototype;
        this.mFactory = factory;

        //resetTable();
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
        Connection c = null;
        try {
            c = getConnection();
            UserDatabaseHelper.insertToTable(c, tableName, p);
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetTable(){
        Connection c = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:"+ this.userName + ".db");
            UserDatabaseHelper.dropTable(c, tableName);
            UserDatabaseHelper.createTable(c, tableName, prototype.newInstance());
            c.close();

        } catch(Exception e){
            System.out.println("Got exception creating table: " + tableName + " e: " + e);
            e.printStackTrace();
        }
    }

    public T readFromBytes(byte[] mBytes){

        System.out.println("Read from bytes: " + Arrays.toString(mBytes));

        T t = null;
        try {
            t = prototype.newInstance();

            System.out.println("t: " + t);

            ByteArrayInputStream mByteStream = new ByteArrayInputStream(mBytes);
            t.readExternal(new DataInputStream(mByteStream), mFactory);
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
        System.out.println("Read ID: " + id);
        byte[] mBytes = readBytes(id);
        return readFromBytes(mBytes);

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getIDsForValue(java.lang.String, java.lang.Object)
     */
    public Vector getIDsForValue(String fieldName, Object value) {

        try {
            Connection c = this.getConnection();
            ResultSet rs = UserDatabaseHelper.selectFromTable(c, this.tableName,
                    new String[]{fieldName}, new String[]{(String)value}, prototype.newInstance());
            Vector<Integer> ids = new Vector<Integer>();
            while(rs.next()){
                ids.add(rs.getInt(TableBuilder.ID_COL));
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
            ResultSet rs = UserDatabaseHelper.selectFromTable(c, this.tableName,
                    new String[]{fieldName}, new String[]{(String)value}, prototype.newInstance());
            if(!rs.next()){
                throw new NoSuchElementException();
            }
            byte[] mBytes = rs.getBytes(TableBuilder.DATA_COL);
            c.close();
            return readFromBytes(mBytes);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
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
            ResultSet rs = UserDatabaseHelper.selectForId(c, this.tableName, id);
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
            ResultSet rs = UserDatabaseHelper.executeSql(c, "SELECT COUNT (*) FROM " + this.tableName + ";");
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

        System.out.println("iterate");

        Connection connection = null;
        ResultSet resultSet = null;

        try {
            connection = this.getConnection();
            resultSet = UserDatabaseHelper.executeSql(connection, "SELECT " + TableBuilder.ID_COL + " , " +
                    TableBuilder.DATA_COL + " FROM " + this.tableName + ";");
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

        System.out.println("Read bytes: " + id);

        try {
            Connection c = getConnection();
            ResultSet rs = UserDatabaseHelper.selectForId(c, this.tableName, id);
            byte[] caseBytes = rs.getBytes(TableBuilder.DATA_COL);
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
        data.remove(DataUtil.integer(id));
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
        data.clear();

        meta.clear();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#removeAll(org.javarosa.core.services.storage.EntityFilter)
     */
    public Vector<Integer> removeAll(EntityFilter ef) {
        Vector<Integer> removed = new Vector<Integer>();
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            Integer i = (Integer)en.nextElement();
            switch (ef.preFilter(i.intValue(), null)) {
                case EntityFilter.PREFILTER_INCLUDE:
                    removed.addElement(i);
                    break;
                case EntityFilter.PREFILTER_EXCLUDE:
                    continue;
            }
            if (ef.matches(data.get(i))) {
                removed.addElement(i);
            }
        }
        for (Integer i : removed) {
            data.remove(i);
        }

        return removed;
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
    public void update(int id, T e) throws StorageFullException {
        data.put(DataUtil.integer(id), e);
    }

    public void setReadOnly() {
        //TODO: This should have a clear contract.
    }


    Vector<String> dynamicIndices = new Vector<String>();

    public void registerIndex(String filterIndex) {
        dynamicIndices.addElement(filterIndex);
    }

    @Override
    public Iterator<T> iterator() {
        return iterate();
    }
}

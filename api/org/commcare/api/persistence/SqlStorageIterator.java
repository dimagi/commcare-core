package org.commcare.api.persistence;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Created by wpride1 on 6/25/15.
 */
public class SqlStorageIterator<E extends Persistable> implements IStorageIterator, Iterator<E> {

    ResultSet resultSet;
    int count = -1;
    SqlIndexedStorageUtility<E> storage;


    public SqlStorageIterator(ResultSet resultSet, int count, SqlIndexedStorageUtility<E> storage){
        this.resultSet = resultSet;
        this.count = count;
        this.storage = storage;
        try {
            resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public int numRecords() {
        return count;
    }



    public int peekID() {
        try {
            return resultSet.getInt(org.commcare.core.database.DatabaseHelper.ID_COL);
        } catch (SQLException e) {
            return -1;
        }
    }


    public int nextID() {
        int nextID = peekID();
        try {
            boolean hasMore = resultSet.next();
            if(!hasMore){
                resultSet.close();
            }
        } catch (SQLException e) {
        }
        return nextID;
    }


    public E nextRecord() {
        byte[] data = new byte[0];
        try {
            data = resultSet.getBytes(org.commcare.core.database.DatabaseHelper.DATA_COL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //we don't really use this
        nextID();
        return storage.readFromBytes(data);
    }


    public boolean hasMore() {
        try {
            return (!resultSet.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean hasNext() {
        return hasMore();
    }


    public E next() {
        return nextRecord();
    }


    public void remove() {
        //unsupported
    }


}

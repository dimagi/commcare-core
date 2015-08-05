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

    @Override
    public int numRecords() {
        return count;
    }


    @Override
    public int peekID() {
        try {
            return resultSet.getInt(UserDatabaseHelper.ID_COL);
        } catch (SQLException e) {
            return -1;
        }
    }

    @Override
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

    @Override
    public E nextRecord() {
        byte[] data = new byte[0];
        try {
            data = resultSet.getBytes(UserDatabaseHelper.DATA_COL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //we don't really use this
        nextID();
        return storage.readFromBytes(data);
    }

    @Override
    public boolean hasMore() {
        try {
            return (!resultSet.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean hasNext() {
        return hasMore();
    }

    @Override
    public E next() {
        return nextRecord();
    }

    @Override
    public void remove() {
        //unsupported
    }


}

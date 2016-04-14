package org.commcare.api.persistence;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Created by wpride1 on 6/25/15.
 */
public class JdbcSqlStorageIterator<E extends Persistable> implements IStorageIterator<E>, Iterator<E> {

    private final PreparedStatement preparedStatement;
    private final ResultSet resultSet;
    private int count = -1;
    private final SqliteIndexedStorageUtility<E> storage;
    private Connection connection;

    public JdbcSqlStorageIterator(PreparedStatement preparedStatement,
                                  ResultSet resultSet,
                                  int count,
                                  SqliteIndexedStorageUtility<E> storage,
                                  Connection conn) {
        this.preparedStatement = preparedStatement;
        this.resultSet = resultSet;
        this.count = count;
        this.storage = storage;
        this.connection = conn;
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
            return resultSet.getInt(org.commcare.modern.database.DatabaseHelper.ID_COL);
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
                preparedStatement.close();
            }
        } catch (SQLException e) {
        }
        return nextID;
    }

    @Override
    public E nextRecord() {
        byte[] data = new byte[0];
        try {
            data = resultSet.getBytes(org.commcare.modern.database.DatabaseHelper.DATA_COL);
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

    public void closeConnection(){
        try {
            if(connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}

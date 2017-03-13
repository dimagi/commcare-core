package org.javarosa.core.services.storage.util;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * @author ctsims
 */
public class DummyStorageIterator<T extends Persistable> implements IStorageIterator<T>, Iterator<T> {
    private int count;
    private final Integer[] keys;
    private final DummyIndexedStorageUtility<T> dummyStorage;

    public DummyStorageIterator(DummyIndexedStorageUtility<T> dummyStorage,
                                Hashtable<Integer, T> data) {
        this.dummyStorage = dummyStorage;
        keys = new Integer[data.size()];
        int i = 0;
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            keys[i] = (Integer)en.nextElement();
            ++i;
        }
        count = 0;
    }

    @Override
    public boolean hasMore() {
        return count < keys.length;
    }

    @Override
    public int nextID() {
        count++;
        return keys[count - 1];
    }

    @Override
    public T nextRecord() {
        return dummyStorage.read(nextID());
    }

    @Override
    public int numRecords() {
        return dummyStorage.getNumRecords();
    }

    @Override
    public int peekID() {
        return keys[count];
    }

    @Override
    public boolean hasNext() {
        return hasMore();
    }

    @Override
    public T next() {
        return nextRecord();
    }

    @Override
    public void remove() {
        // not implemented
    }
}

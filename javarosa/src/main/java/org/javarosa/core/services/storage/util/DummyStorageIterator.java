package org.javarosa.core.services.storage.util;

import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.DataUtil;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author ctsims
 */
public class DummyStorageIterator<T extends Persistable> implements IStorageIterator<T> {
    private final Hashtable<Integer, T> data;
    private int count;
    private final Integer[] keys;

    public DummyStorageIterator(Hashtable<Integer, T> data) {
        this.data = data;
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
        return data.get(DataUtil.integer(nextID()));
    }

    @Override
    public int numRecords() {
        return data.size();
    }

    @Override
    public int peekID() {
        return keys[count];
    }
}

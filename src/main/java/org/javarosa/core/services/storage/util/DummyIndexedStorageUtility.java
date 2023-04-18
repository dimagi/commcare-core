package org.javarosa.core.services.storage.util;

import org.javarosa.core.model.condition.RequestAbandonedException;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class DummyIndexedStorageUtility<T extends Persistable> implements IStorageUtilityIndexed<T> {

    private Hashtable<String, Hashtable<Object, Vector<Integer>>> meta = new Hashtable<>();
    private final Hashtable<Integer, T> data = new Hashtable<>();
    private int curCount = 0;
    private final Class<T> prototype;
    private final PrototypeFactory mFactory;

    public DummyIndexedStorageUtility(Class<T> prototype, PrototypeFactory factory) {
        this.prototype = prototype;
        this.mFactory = factory;
        initMetaFromClass();
    }

    public DummyIndexedStorageUtility(T instance, PrototypeFactory factory) {
        this.prototype = (Class<T>)instance.getClass();
        this.mFactory = factory;
        initMetaFromInstance(instance);
    }

    private void initMetaFromClass() {
        Persistable p;
        try {
            p = prototype.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Couldn't create a serializable class for storage!" + prototype.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't create a serializable class for storage!" + prototype.getName());
        }

        initMetaFromInstance(p);
    }

    private void initMetaFromInstance(Persistable p) {
        if (!(p instanceof IMetaData)) {
            return;
        }
        IMetaData m = (IMetaData)p;
        for (String key : m.getMetaDataFields()) {
            if (!meta.containsKey(key)) {
                meta.put(key, new Hashtable<Object, Vector<Integer>>());
            }
        }
    }

    private Vector<Integer> getIDsForInverseValue(String fieldName, Object value) {
        if (meta.get(fieldName) == null) {
            throw new IllegalArgumentException("Unsupported index: " + fieldName + " for storage of " + prototype.getName());
        }
        Hashtable<Object, Vector<Integer>> allValues = meta.get(fieldName);
        Vector<Integer> ids = new Vector<>();
        for (Enumeration en = allValues.keys(); en.hasMoreElements(); ) {
            Object key = en.nextElement();

            if (!key.equals(value) && allValues.get(key) != null) {
                for (Integer id : allValues.get(key)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    @Override
    public Vector<Integer> getIDsForValue(String fieldName, Object value) {
        //We don't support all index types
        if (meta.get(fieldName) == null) {
            throw new IllegalArgumentException("Unsupported index: " + fieldName + " for storage of " + prototype.getName());
        }
        if (meta.get(fieldName).get(value) == null) {
            return new Vector<>();
        }
        return meta.get(fieldName).get(value);
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values) {
        return getIDsForValues(fieldNames, values, new LinkedHashSet<Integer>());
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values, LinkedHashSet<Integer> returnSet) {
        return getIDsForValues(fieldNames, values, new String[0], new Object[0], returnSet);
    }

    @Override
    public List<Integer> getIDsForValues(String[] fieldNames, Object[] values, String[] inverseMatchFields, Object[] inverseMatchValues, LinkedHashSet<Integer> returnSet) {
        if (fieldNames.length == 0 && inverseMatchFields.length == 0) {
            List<Integer> matches = new ArrayList<>(data.keySet());
            returnSet.addAll(data.keySet());
            return matches;
        }

        List<Integer> accumulator = null;
        for (int i = 0; i < fieldNames.length; ++i) {
            Vector<Integer> matches = getIDsForValue(fieldNames[i], values[i]);
            if (accumulator == null) {
                accumulator = new Vector<>(matches);
            } else {
                accumulator = DataUtil.intersection(accumulator, matches);
            }
        }
        for (int i = 0; i < inverseMatchFields.length; ++i) {
            Vector<Integer> matches = getIDsForInverseValue(inverseMatchFields[i], inverseMatchValues[i]);
            if (accumulator == null) {
                accumulator = new Vector<>(matches);
            } else {
                accumulator = DataUtil.intersection(accumulator, matches);
            }
        }

        returnSet.addAll(accumulator);
        return accumulator;
    }

    @Override
    public T getRecordForValue(String fieldName, Object value) throws NoSuchElementException, InvalidIndexException {
        if (meta.get(fieldName) == null) {
            throw new NoSuchElementException("No record matching meta index " + fieldName + " with value " + value);
        }

        Vector<Integer> matches = meta.get(fieldName).get(value);

        if (matches == null || matches.size() == 0) {
            throw new NoSuchElementException("No record matching meta index " + fieldName + " with value " + value);
        }
        if (matches.size() > 1) {
            throw new InvalidIndexException("Multiple records matching meta index " + fieldName + " with value " + value, fieldName);
        }

        return read(matches.elementAt(0));
    }

    @Override
    public Vector<T> getRecordsForValues(String[] metaFieldNames, Object[] values) {
        Vector<T> matches = new Vector<>();
        List<Integer> idMatches = getIDsForValues(metaFieldNames, values);
        for (Integer id : idMatches) {
            matches.add(read(id));
        }
        return matches;
    }

    @Override
    public int add(T e) {
        data.put(DataUtil.integer(curCount), e);
        addMeta(curCount);

        // This is not a legit pair of operations;
        curCount++;

        return curCount - 1;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean exists(int id) {
        return data.containsKey(DataUtil.integer(id));
    }

    @Override
    public Object getAccessLock() {
        return null;
    }

    @Override
    public int getNumRecords() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.size() > 0;
    }

    @Override
    public IStorageIterator<T> iterate() {
        //We should really find a way to invalidate old iterators first here
        return new DummyStorageIterator<>(this, data);
    }

    @Override
    public IStorageIterator<T> iterate(boolean includeData) {
        return iterate();
    }


    @Override
    public T read(int id) {
        try {
            T t = prototype.newInstance();
            t.readExternal(new DataInputStream(new ByteArrayInputStream(readBytes(id))), mFactory);
            t.setID(id);
            return t;
        } catch (IllegalAccessException | InstantiationException | IOException | DeserializationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public byte[] readBytes(int id) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            T item = data.get(DataUtil.integer(id));
            if (item != null) {
                item.writeExternal(new DataOutputStream(stream));
                return stream.toByteArray();
            } else {
                throw new NoSuchElementException("No record for ID " + id);
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't serialize data to return to readBytes");
        }
    }

    @Override
    public void remove(int id) {
        data.remove(DataUtil.integer(id));

        syncMeta();
    }

    @Override
    public void remove(Persistable p) {
        remove(p.getID());
    }

    @Override
    public void removeAll() {
        data.clear();
    }

    @Override
    public Vector<Integer> removeAll(EntityFilter ef) {
        Vector<Integer> removed = new Vector<>();
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            Integer i = (Integer)en.nextElement();
            switch (ef.preFilter(i, null)) {
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

        syncMeta();

        return removed;
    }

    @Override
    public void update(int id, T e) {
        data.put(DataUtil.integer(id), e);
        addMeta(DataUtil.integer(id));
    }

    @Override
    public void write(Persistable p) {
        if (p.getID() != -1) {
            this.data.put(DataUtil.integer(p.getID()), (T)p);
            addMeta(DataUtil.integer(p.getID()));
        } else {
            p.setID(curCount);
            this.add((T)p);
        }
    }

    private void syncMeta() {
        for (Hashtable<Object, Vector<Integer>> metaEntry : meta.values()) {
            metaEntry.clear();
        }

        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            Integer i = (Integer)en.nextElement();
            addMeta(i);
        }
    }

    private void addMeta(Integer i) {
        Externalizable e = data.get(i);

        if (e instanceof IMetaData) {
            IMetaData m = (IMetaData)e;
            for (Enumeration keys = meta.keys(); keys.hasMoreElements(); ) {
                String key = (String)keys.nextElement();

                Object value = m.getMetaData(key);
                if (value == null) {
                    continue;
                }

                Hashtable<Object, Vector<Integer>> records = meta.get(key);

                if (!records.containsKey(value)) {
                    records.put(value, new Vector<Integer>());
                }
                Vector<Integer> indices = records.get(value);
                if (!indices.contains(i)) {
                    indices.add(i);
                }
            }
        }
    }

    @Override
    public void bulkRead(LinkedHashSet<Integer> cuedCases, HashMap<Integer, T> recordMap) throws RequestAbandonedException {
        for (int i : cuedCases) {
            recordMap.put(i, data.get(i));
        }
    }

    @Override
    public String[] getMetaDataForRecord(int recordId, String[] fieldNames) {
        String[] response = new String[fieldNames.length];
        for (int i = 0; i < fieldNames.length; ++i) {
            response[i] = (String)((IMetaData)data.get(recordId)).getMetaData(fieldNames[i]);
        }
        return response;
    }

    @Override
    public Vector<T> getBulkRecordsForIndex(String metaFieldName, Collection<String> matchingValues) {
        // we don't care about bulk retrieval for dummy storage, so just call normal method to get records here
        return getRecordsForValues(new String[]{metaFieldName}, matchingValues.toArray());
    }

    @Override
    public void bulkReadMetadata(LinkedHashSet cuedCases, String[] metaDataIds, HashMap metadataMap) {
        for (int i : ((LinkedHashSet<Integer>)cuedCases)) {
            metadataMap.put(i, getMetaDataForRecord(i, metaDataIds));
        }
    }

    @Override
    public Class<?> getPrototype() {
        return prototype;
    }

    @Override
    public boolean isStorageExists() {
        return meta != null;
    }

    @Override
    public void initStorage() {
        meta = new Hashtable<>();
    }

    @Override
    public void deleteStorage() {
        meta = null;
    }
}

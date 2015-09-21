package org.javarosa.j2me.storage.rms;

import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.MetaDataWrapper;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.j2me.storage.rms.raw.RMSFactory;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

/* TEMPORARY / EXPERIMENTAL */

public class RMSStorageUtilityIndexed<E extends Externalizable> extends RMSStorageUtility<E> implements IStorageUtilityIndexed<E> {

    private Object metadataAccessLock = new Object();
    Hashtable<String, Hashtable<Object, Vector<Integer>>> metaDataIndex = null;
    boolean hasMetaData;
    IMetaData proto;
    Vector<String> dynamicIndices = new Vector<String>();

    public RMSStorageUtilityIndexed (String basename, Class type) {
        super(basename, type);
        init(type);
    }

    public RMSStorageUtilityIndexed (String basename, Class type, boolean allocateIDs, RMSFactory factory) {
        super(basename, type, allocateIDs, factory);
        init(type);
    }

    private void init (Class type) {
        hasMetaData = IMetaData.class.isAssignableFrom(type);
        if (hasMetaData) {
            proto = (IMetaData)PrototypeFactory.getInstance(type);
        }
    }

    private void checkIndex () {
        synchronized(metadataAccessLock) {
            if (metaDataIndex == null) {
                buildIndex();
            }
        }
    }

    private void buildIndex () {
        synchronized(metadataAccessLock) {
            //cts: We used to turn off interning here, but it's unclear whether it was useful
            //in very many environments. We should re-profile on bad Garbage collectors and check
            //again.
            try{

                metaDataIndex = new Hashtable();

                if (!hasMetaData) {
                    return;
                }

                String[] fields = getFields();
                for (int k = 0; k < fields.length; k++) {
                    metaDataIndex.put(fields[k], new Hashtable());
                }


                IStorageIterator i = iterate();
                int records = this.getNumRecords();
                Hashtable[] metadata = new Hashtable[records];
                int[] recordIds = new int[records];
                for(int j = 0 ; j < records ; ++j) {
                    metadata[j] = new Hashtable(fields.length);
                    for(String field : fields) {
                        metadata[j].put(field, "");
                    }
                }
                int count = 0;
                IMetaData obj;
                while (i.hasMore()) {
                    recordIds[count] = i.nextID();
                    count++;
                }

                //0 memory allocation zone
                for(int index = 0 ; index < recordIds.length; ++ index) {
                    obj = (IMetaData)read(recordIds[index]);

                    copyHT(metadata[index], getMetaData(obj, fields), fields);

                    obj = null;
                    System.gc();
                }
                //0 memory allocation zone
                for(int index = 0; index < recordIds.length; ++index) {
                    indexMetaData(recordIds[index], metadata[index]);
                }
            } finally{
                //we used to have to revert the interning flip here. If we need to re-introduce
                //not-interning during this period, this is where we should revert it.
            }
        }
    }

    private int i = 0;
    private void copyHT(Hashtable into, Hashtable source, String[] fields) {
        for(i = 0; i < fields.length ; ++i) {
            into.put(fields[i], source.get(fields[i]));
        }
    }

    private void indexMetaData (int id, Hashtable vals) {
        for (Enumeration e = vals.keys(); e.hasMoreElements(); ) {
            String field = (String)e.nextElement();
            Object val = vals.get(field);

            Vector IDs = getIDList(field, val);
            if (IDs.contains(DataUtil.integer(id))) {
                System.out.println("warning: don't think this should happen [add] [" + id + ":" + field + ":" + val.toString() + "]");
            }
            IDs.addElement(DataUtil.integer(id));
        }
    }

    private void removeMetaData (int id, IMetaData obj) {
        Hashtable vals = getMetaData(obj);
        for (Enumeration e = vals.keys(); e.hasMoreElements(); ) {
            String field = (String)e.nextElement();
            Object val = vals.get(field);

            Vector IDs = getIDList(field, val);
            if (!IDs.contains(new Integer(id))) {
                System.out.println("warning: don't think this should happen [remove] [" + id + ":" + field + ":" + val.toString() + "]");
            }
            IDs.removeElement(new Integer(id));
            if (IDs.size() == 0) {
                ((Hashtable)(metaDataIndex.get(field))).remove(val);
            }
        }
    }

    private Vector getIDList (String field, Object value) {
        Vector IDs;
        synchronized(metadataAccessLock) {
            IDs = (Vector)((Hashtable)(metaDataIndex.get(field))).get(value);
            if (IDs == null) {
                IDs = new Vector();
                ((Hashtable)(metaDataIndex.get(field))).put(value, IDs);
            }
        }
        return IDs;
    }

    public void write (Persistable p) {
        IMetaData old = null;
        synchronized(metadataAccessLock) {
            if (hasMetaData) {
                checkIndex();
                if (exists(p.getID())) {
                    old = getMetaDataForRecord(p.getID());
                }
            }

            super.write(p);

            if (hasMetaData) {
                if (old != null) {
                    removeMetaData(p.getID(), old);
                }
                indexMetaData(p.getID(), getMetaData(((IMetaData)p)));
            }
        }
    }

    private IMetaData getMetaDataForRecord(int record) {
        Hashtable<String, Object> data = null;
        synchronized(metadataAccessLock) {
            if (hasMetaData) {
                if (exists(record)) {
                    data = new Hashtable<String, Object>();
                    Integer recordId = DataUtil.integer(record);
                    for(String s : getFields()) {
                        Hashtable<Object, Vector<Integer>> values = metaDataIndex.get(s);
                        for(Enumeration en = values.keys() ; en.hasMoreElements();) {
                            Object o = en.nextElement();
                            Vector<Integer> ids = values.get(o);
                            if(ids.contains(recordId)){
                                data.put(s, o);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return new MetaDataWrapper(data);
    }

    public int add (E e) {
        synchronized(metadataAccessLock) {
            if (hasMetaData) {
                checkIndex();
            }

            int id = super.add(e);

            if (hasMetaData) {
                indexMetaData(id, getMetaData(((IMetaData)e)));
            }
            return id;
        }
    }

    public void update (int id, E e) {
        synchronized(metadataAccessLock) {

            Externalizable old;
            if (hasMetaData) {
                old = read(id);
                checkIndex();
                removeMetaData(id, (IMetaData)old);
            }

            super.update(id, e);

            if (hasMetaData) {
                indexMetaData(id, getMetaData((IMetaData)e));
            }
        }
    }

    public void remove (int id) {
        synchronized(metadataAccessLock) {
            IMetaData old = null;
            if (hasMetaData) {
                checkIndex();
                old = getMetaDataForRecord(id);
            }

            super.remove(id);

            if (hasMetaData) {
                removeMetaData(id, old);
            }
        }
    }

    public Vector getIDsForValue (String fieldName, Object value) {
        synchronized(metadataAccessLock) {
            checkIndex();

            Hashtable index = (Hashtable)metaDataIndex.get(fieldName);
            if (index == null) {
                throw new IllegalArgumentException("field [" + fieldName + "] not recognized");
            }

            Vector IDs = (Vector)index.get(value);
            return (IDs == null ? new Vector(): IDs);
        }
    }

    public E getRecordForValue (String fieldName, Object value) throws NoSuchElementException {
        synchronized(metadataAccessLock) {
            Vector IDs = getIDsForValue(fieldName, value);
            if (IDs.size() == 1) {
                return read(((Integer)IDs.elementAt(0)).intValue());
            } else if (IDs.size() == 0){
                throw new NoSuchElementException("Storage utility " + getName() +  " does not contain any records with the index " + fieldName + " equal to " + value.toString());
            } else {
                throw new InvalidIndexException(fieldName + " is not a valid unique index. More than one record was found with value [" + value.toString() + "] in field [" + fieldName + "]",fieldName);
            }
        }
    }

    public void clearIndexCache() {
        synchronized(metadataAccessLock) {
            if(metaDataIndex != null) {
                metaDataIndex = null;
            }
        }
    }

    public void registerIndex(String index) {
        if(index == null) { throw new NullPointerException("Null index registration attempt!");}
        synchronized(metadataAccessLock) {
            if(dynamicIndices == null) { dynamicIndices = new Vector<String>(); }
            dynamicIndices.addElement(index);
            buildIndex();
        }
    }

    private Hashtable<String, Object> getMetaData(IMetaData m) {
        return getMetaData(m, getFields());
    }
    private Hashtable<String, Object> getMetaData(IMetaData m, String[] index) {
        Hashtable<String, Object> h = new Hashtable<String, Object>();
        for(String s : index) {
            Object o = m.getMetaData(s);
            if(o == null || s == null) {
                "sadf".charAt(0);
            }
            h.put(s, o);
        }
        return h;
    }
    private String[] getFields() {
        String[] mdfields = proto.getMetaDataFields();
        String[] fields;
        if(dynamicIndices != null) {
            fields = new String[mdfields.length + dynamicIndices.size()];
            for(int i = 0 ; i < mdfields.length; ++i) {
                fields[i] = mdfields[i];
            }
            int count = mdfields.length;
            for (int k = 0; k < dynamicIndices.size(); k++) {
                fields[count] = dynamicIndices.elementAt(k);
                count++;
            }
        } else {
            fields = mdfields;
        }
        return fields;
    }
}


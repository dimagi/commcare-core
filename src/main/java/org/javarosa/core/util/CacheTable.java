package org.javarosa.core.util;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A Cache Table is a self-purging weak reference store that can be used
 * to maintain a cache of objects keyed by a dynamic type.
 *
 * Cache tables will automatically clean up references to freed week references
 * on an internal schedule, and compact the table size to maintain as small
 * of a footprint as possible.
 *
 * Cache tables are only available with Weak References to maintain compatibility
 * with j2me runtimes.
 *
 * @author ctsims
 */
public class CacheTable<T, K> {
    private int totalAdditions = 0;

    private Hashtable<T, WeakReference> currentTable;

    private static final ThreadLocal<Vector<WeakReference>> caches = new ThreadLocal<Vector<WeakReference>>() {
        @Override
        protected Vector<WeakReference> initialValue()
        {
            return new Vector<>();
        }
    };

    private static final Thread cleaner = new Thread(new Runnable() {
        @Override
        public void run() {
            Vector<Integer> toRemove = new Vector<>();
            while (true) {
                try {
                    toRemove.removeAllElements();
                    for (int i = 0; i < caches.get().size(); ++i) {
                        CacheTable cache = (CacheTable)caches.get().elementAt(i).get();
                        if (cache == null) {
                            toRemove.addElement(DataUtil.integer(i));
                        } else {
                            Hashtable<Object, WeakReference> table = cache.currentTable;
                            for (Enumeration en = table.keys(); en.hasMoreElements(); ) {
                                Object key = en.nextElement();

                                synchronized (cache) {
                                    //See whether or not the cached reference has been cleared by the GC
                                    if (table.get(key).get() == null) {
                                        //If so, remove the entry, it's no longer useful.
                                        table.remove(key);
                                    }
                                }
                            }

                            synchronized (cache) {
                                //See if our current size is 25% the size of the largest size we've been
                                //and compact (clone to a new table) if so, since the table maintains the
                                //largest size it has ever been.
                                //TODO: 50 is a super arbitrary upper bound
                                if (cache.totalAdditions > 50 && cache.totalAdditions - cache.currentTable.size() > (cache.currentTable.size() >> 2)) {
                                    Hashtable newTable = new Hashtable(cache.currentTable.size());
                                    int oldMax = cache.totalAdditions;
                                    for (Enumeration en = table.keys(); en.hasMoreElements(); ) {
                                        Object key = en.nextElement();
                                        newTable.put(key, cache.currentTable.get(key));
                                    }
                                    cache.currentTable = newTable;
                                    cache.totalAdditions = cache.currentTable.size();
                                }
                            }

                        }
                    }
                    for (int id = toRemove.size() - 1; id >= 0; --id) {
                        caches.get().removeElementAt(toRemove.elementAt(id));
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    });

    private static void registerCache(CacheTable table) {
        caches.get().addElement(new WeakReference(table));
        synchronized (cleaner) {
            if (!cleaner.isAlive()) {
                cleaner.start();
            }
        }
    }

    public CacheTable() {
        super();
        currentTable = new Hashtable<>();
        registerCache(this);
    }


    public K retrieve(T key) {
        synchronized (this) {
            if (!currentTable.containsKey(key)) {
                return null;
            }
            K retVal = (K)currentTable.get(key).get();
            if (retVal == null) {
                currentTable.remove(key);
            }
            return retVal;
        }
    }

    public void register(T key, K item) {
        synchronized (this) {
            currentTable.put(key, new WeakReference(item));
            totalAdditions++;
        }
    }

    public void clear(){
        currentTable.clear();
        caches.get().removeAllElements();
    }
}

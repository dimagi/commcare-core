package org.javarosa.core.util;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A Local Cache Table is a weak reference store that can be used
 * to maintain a cache of objects keyed by a dynamic type.
 *
 * Local Cache Tables should be used for non-global caches. For global caches
 * use {@link CacheTable}.
 */
public class LocalCacheTable<T, K> {
    private Hashtable<T, WeakReference> currentTable = new Hashtable<>();

    public LocalCacheTable() {
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
        }
    }

    public void clear(){
        currentTable.clear();
    }
}

package org.javarosa.core.util;

/**
 * An Interner is a special case of a a cache table that is used to intern objects
 * which will exist in multiple contexts at runtime. All of the keys in an interner
 * are integer values.
 *
 * @author ctsims
 */
public class Interner<K> extends CacheTable<Integer, K> {

    public K retrieve(int key) {
        synchronized (this) {
            return super.retrieve(DataUtil.integer(key));
        }
    }

    public void register(int key, K item) {
        synchronized (this) {
            super.register(DataUtil.integer(key), item);
        }
    }
}

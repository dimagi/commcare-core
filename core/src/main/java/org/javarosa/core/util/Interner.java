package org.javarosa.core.util;

/**
 * An Interner is a special case of a a cache table that is used to intern objects
 * which will exist in multiple contexts at runtime. All of the keys in an interner
 * are integer values.
 *
 * @author ctsims
 */
public class Interner<K> extends CacheTable<Integer, K> {

    /**
     * Intern the provided value in this cache table
     *
     * @param k The object to be interned
     * @return Either the original object (if it has been interned by this operation) or
     * an object that is identical to the one passed in. Note that the hashcode and equals
     * methods of the object type need to be correctly implemented for interning to function
     * as expected.
     */
    public K intern(K k) {
        synchronized (this) {
            int hash = k.hashCode();
            K nk = retrieve(hash);
            if (nk == null) {
                register(hash, k);
                return k;
            }

            if (k.equals(nk)) {
                return nk;
            } else {
                //Collision. We should deal with this better for interning (and not manually caching) tables.
                return k;
            }
        }
    }

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

package org.javarosa.core.util;

import java.util.Vector;

/**
 * @author Clayton Sims
 */
public class ArrayUtilities {
    public static boolean arraysEqual(Object[] array1, Object[] array2) {
        if (array1.length != array2.length) {
            return false;
        }

        for (int i = 0; i < array1.length; ++i) {
            if (!array1[i].equals(array2[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean arraysEqual(byte[] array1, byte[] array2) {
        if (array1.length != array2.length) {
            return false;
        }

        for (int i = 0; i < array1.length; ++i) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find a single intersecting element common to two lists, or null if none
     * exists. Note that no unique condition will be reported if there are multiple
     * elements which intersect, so this should likely only be used if the possible
     * size of intersection is 0 or 1
     */
    public static <E> E intersectSingle(Vector<E> a, Vector<E> b) {
        for (E e : a) {
            if (b.indexOf(e) != -1) {
                return e;
            }
        }
        return null;
    }

    public static <E> Vector<E> vectorCopy(Vector<E> a) {
        if (a == null) {
            return null;
        }
        Vector<E> b = new Vector<>();
        for (E e : a) {
            b.addElement(e);
        }
        return b;
    }

    public static <E> E[] copyIntoArray(Vector<E> v, E[] a) {
        int i = 0;
        for (E e : v) {
            a[i++] = e;
        }
        return a;
    }

    public static <E> Vector<E> toVector(E[] a) {
        Vector<E> v = new Vector<>();
        for (E e : a) {
            v.addElement(e);
        }
        return v;
    }
}

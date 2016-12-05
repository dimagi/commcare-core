package org.javarosa.core.util;

import java.util.HashSet;
import java.util.Vector;

/**
 * @author ctsims
 */
public class DataUtil {
    private static final int offset = 10;
    private static final int low = -10;
    private static final int high = 400;
    private static Integer[] iarray;

    /**
     * Get Integer object that corresponds to int argument from a
     * pre-computed table, or build a new instance.
     *
     * @return Cached or new Integer instance that correpsonds to ivalue argument
     */
    public static Integer integer(int ivalue) {
        // lazily populate Integer cache
        if (iarray == null) {
            iarray = new Integer[high - low];
            for (int i = 0; i < iarray.length; ++i) {
                iarray[i] = i + low;
            }
        }

        if (ivalue < high && ivalue >= low) {
            return iarray[ivalue + offset];
        } else {
            return ivalue;
        }
    }

    public static <T> Vector<T> intersection(Vector<T> a, Vector<T> b) {
        HashSet<T> setA = new HashSet<>(a);
        HashSet<T> setB = new HashSet<>(b);
        setA.retainAll(setB);
        return new Vector<>(setA);
    }

    public static String[] splitOnSpaces(String s) {
        if ("".equals(s)) {
            return new String[0];
        }
        return s.split("[ ]+");
    }

    public static String[] splitOnDash(String s) {
        return s.split("-");
    }

    public static String[] splitOnColon(String s) {
        return s.split(":");
    }

    public static String[] splitOnPlus(String s) {
        return s.split("[+]");
    }
}

package org.javarosa.core.util;

import java.util.Vector;

/**
 * @author ctsims
 */
public class DataUtil {
    static final int offset = 10;
    static final int low = -10;
    static final int high = 400;
    static Integer[] iarray;

    static IntersectionLambda intersectionLambda = new IntersectionLambda();

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
                iarray[i] = new Integer(i + low);
            }
        }

        if (ivalue < high && ivalue >= low) {
            return iarray[ivalue + offset];
        } else {
            return new Integer(ivalue);
        }
    }

    public static <T> Vector<T> intersection(Vector<T> a, Vector<T> b) {
        return intersectionLambda.intersection(a, b);
    }

    public static void setIntersectionLambda(IntersectionLambda newIntersectionLambda) {
        intersectionLambda = newIntersectionLambda;
    }

    public static class IntersectionLambda {
        public <T> Vector<T> intersection(Vector<T> a, Vector<T> b) {
            Vector<T> u = new Vector<>();
            for (T i : a) {
                if (b.contains(i)) {
                    u.addElement(i);
                }
            }
            return u;
        }
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

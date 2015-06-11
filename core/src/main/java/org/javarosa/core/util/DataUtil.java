/**
 *
 */
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

    static UnionLambda unionLambda = new UnionLambda();

    public static Integer integer(int ivalue) {
        if (iarray == null) {
            iarray = new Integer[high - low];
            for (int i = 0; i < iarray.length; ++i) {
                iarray[i] = new Integer(i + low);
            }
        }
        return ivalue < high && ivalue >= low ? iarray[ivalue + offset] : new Integer(ivalue);
    }


    public static <T> Vector<T> union(Vector<T> a, Vector<T> b) {
        return unionLambda.union(a, b);
    }

    public static void setUnionLambda(UnionLambda newUnionLambda) {
        unionLambda = newUnionLambda;
    }

    public static class UnionLambda {
        public <T> Vector<T> union(Vector<T> a, Vector<T> b) {
            Vector<T> u = new Vector<T>();
            //Efficiency?
            for (T i : a) {
                if (b.contains(i)) {
                    u.addElement(i);
                }
            }
            return u;
        }
    }
}

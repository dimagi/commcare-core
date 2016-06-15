package org.javarosa.core.util;

import org.javarosa.core.model.utils.DateUtils;

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
    static StringSplitter stringSplitter = new StringSplitter();

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

    public static String[] splitOnSpaces(String s) {
        return stringSplitter.splitOnSpaces(s);
    }

    public static String[] splitOnDash(String s) {
        return stringSplitter.splitOnDash(s);
    }

    public static String[] splitOnColon(String s) {
        return stringSplitter.splitOnColon(s);
    }

    public static String[] splitOnPlus(String s) {
        return stringSplitter.splitOnPlus(s);
    }

    public static void setStringSplitter(StringSplitter newStringSplitter) {
        stringSplitter = newStringSplitter;
    }

    public static class StringSplitter {

        public String[] splitOnSpaces(String s) {
            Vector<String> vectorSplit = split(s, " ", true);
            return vectorSplit.toArray(new String[vectorSplit.size()]);
        }

        public String[] splitOnDash(String s) {
            Vector<String> vectorSplit = split(s, "-", false);
            return vectorSplit.toArray(new String[vectorSplit.size()]);
        }

        public String[] splitOnColon(String s) {
            Vector<String> vectorSplit = split(s, ":", false);
            return vectorSplit.toArray(new String[vectorSplit.size()]);
        }

        public String[] splitOnPlus(String s) {
            Vector<String> vectorSplit = split(s, "+", false);
            return vectorSplit.toArray(new String[vectorSplit.size()]);
        }
    }

    /**
     * Custom implementation of tokenizing a string based on a delimiter, for use in j2me
     * (Java's String.split() method was not available until Java 1.4)
     *
     * @param str                       The string to be split
     * @param delimiter                 The delimeter to be used
     * @param combineMultipleDelimiters If two delimiters occur in a row,
     *                                  remove the empty strings created by their split
     * @return A vector of strings contained in original which were separated by the delimiter
     */
    public static Vector<String> split(String str, String delimiter, boolean combineMultipleDelimiters) {
        Vector<String> pieces = new Vector<String>();

        int index = str.indexOf(delimiter);
        // add all substrings, split by delimiter, to pieces.
        while (index >= 0) {
            pieces.addElement(str.substring(0, index));
            str = str.substring(index + delimiter.length());
            index = str.indexOf(delimiter);
        }
        pieces.addElement(str);

        // remove all pieces that are empty string
        if (combineMultipleDelimiters) {
            for (int i = 0; i < pieces.size(); i++) {
                if (pieces.elementAt(i).length() == 0) {
                    pieces.removeElementAt(i);
                    i--;
                }
            }
        }

        return pieces;
    }

}

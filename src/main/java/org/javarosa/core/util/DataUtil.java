package org.javarosa.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

    public static <T> List<T> intersection(Collection<T> a, Collection<T> b) {
        if (b.size() < a.size()) {
            return intersection(b, a);
        }

        HashSet<T> setA = a instanceof HashSet ? (HashSet<T>)((HashSet<T>)a).clone() : new HashSet<>(a);
        HashSet<T> setB = b instanceof HashSet ? (HashSet<T>)b : new HashSet<>(b);
        setA.retainAll(setB);
        return new Vector<>(setA);
    }

    public static String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s + " ");
        }
        return sb.toString().substring(0, sb.length()-1);
    }

    public static List<String> stringToList(String s) {
        return Arrays.asList(splitOnSpaces(s));
    }

    public static String[] splitOnSpaces(String s) {
        if ("".equals(s)) {
            return new String[0];
        }
        return s.split("[ ]+");
    }

    public static boolean intArrayContains(int[] source, int target) {
        for (int current: source) {
            if (current == target) {
                return true;
            }
        }
        return false;
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

    public static boolean isImageFile(String filename){
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg") ||
                lowerFilename.endsWith(".png") || lowerFilename.endsWith(".gif") ||
                lowerFilename.endsWith(".bmp") || lowerFilename.endsWith(".webp");
    }
}

package org.commcare.util;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by shubham on 07/11/17.
 */

/**
 * Common operations on Collections
 */
public class CollectionUtils {

    /**
     * @param first  First Integer Vector to be merged
     * @param second Second Integer Vector to be merged
     * @return single merged int array of first and second
     */
    public static int[] mergeIntegerVectorsInArray(Vector<Integer> first, Vector<Integer> second) {
        int resultLength = first.size() + second.size();
        int[] result = new int[resultLength];
        int i = 0;
        for (; i < first.size(); ++i) {
            result[i] = first.elementAt(i);
        }
        for (; i < resultLength; ++i) {
            result[i] = second.elementAt(i - first.size());
        }
        return result;
    }

    /**
     * Checks if any element in {@code subList} is contained in {@code superList}
     * @param superList
     * @param subList
     * @return true if {@code superList} contains any element of {@code subList}, false otherwise
     */
    public static boolean containsAny(ArrayList<String> superList, ArrayList<String> subList) {
        for (String item : subList) {
            if(superList.contains(item)){
                return true;
            }
        }
        return false;
    }
}

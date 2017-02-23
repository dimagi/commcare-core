package org.javarosa.core.util.test;

import org.javarosa.core.util.DataUtil;
import org.junit.Test;

import java.util.Collection;
import java.util.Vector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class DataUtilTest {
    @Test
    public void intersectionTest() {
        Vector<String> setOne = new Vector<>();
        setOne.add("one");
        setOne.add("two");

        Vector<String> setTwo = new Vector<>();
        setTwo.add("one");
        setTwo.add("three");
        Collection<String> intersectSet = DataUtil.intersection(setOne, setTwo);

        // for safety, we want to return a whole new vector
        assertFalse(intersectSet == setOne);
        assertFalse(intersectSet == setTwo);

        // for safety, don't modify ingoing vector arguments
        assertTrue(setOne.contains("one"));
        assertTrue(setOne.contains("two"));

        assertTrue(setTwo.contains("one"));
        assertTrue(setTwo.contains("three"));

        // make sure proper intersection is computed
        assertTrue(intersectSet.contains("one"));

        assertFalse(intersectSet.contains("two"));
        assertFalse(intersectSet.contains("three"));
    }
}

package org.javarosa.core.util.test;

import org.javarosa.core.util.DataUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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

    @Test
    public void splitOnSpacesTest() {
        assertEquals(DataUtil.splitOnSpaces("a b c"), new String[]{"a", "b", "c"});
        assertEquals(DataUtil.splitOnSpaces(""), new String[0]);
        assertEquals(DataUtil.splitOnSpaces("texas \"new mexico\" utah"), new String[]{"texas", "new mexico", "utah"});
        assertEquals(DataUtil.splitOnSpaces("'things' 'other things'"), new String[]{"things", "other things"});
        assertEquals(DataUtil.splitOnSpaces("\"Jenny\'s teapot\" \"Ethan\'s mug\""), new String[]{"Jenny's teapot", "Ethan's mug"});
        assertEquals(DataUtil.splitOnSpaces(" blank"), new String[]{"", "blank"});
    }

    @Test
    public void joinWithSpacesTest() {
        assertEquals(DataUtil.joinWithSpaces(new String[]{"a", "b", "c"}), "a b c");
        assertEquals(DataUtil.joinWithSpaces(new String[0]), "");
        assertEquals(DataUtil.joinWithSpaces(new String[]{"texas", "new mexico", "utah"}), "texas \"new mexico\" utah");
        assertEquals(DataUtil.joinWithSpaces(new String[]{"they said \"hello\"", "thing"}), "\"they said \\\"hello\\\"\" thing");
        assertEquals(DataUtil.joinWithSpaces(new String[]{"", "blank"}), " blank");
    }
}

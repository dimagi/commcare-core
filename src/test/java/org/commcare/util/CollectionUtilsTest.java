package org.commcare.util;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class CollectionUtilsTest {

    @Test
    public void containsAny() {
        ArrayList<String> superList = new ArrayList();
        superList.add("Apple");
        superList.add("Mango");
        superList.add("Banana");

        assertFalse("Empty List can't contain any fruits from superList",
                CollectionUtils.containsAny(superList, new ArrayList<>()));

        ArrayList<String> subListOne = new ArrayList<>();
        subListOne.add("Orange");

        assertFalse("List doesn't contain any fruits from superList",
                CollectionUtils.containsAny(superList, subListOne));

        ArrayList<String> subListTwo = new ArrayList<>();
        subListTwo.add("Orange");
        subListTwo.add("Mango");

        assertTrue("List contains Mango from superList",
                CollectionUtils.containsAny(superList, subListTwo));
    }
}

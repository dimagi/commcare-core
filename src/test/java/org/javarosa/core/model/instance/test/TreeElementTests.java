package org.javarosa.core.model.instance.test;

import org.javarosa.core.model.instance.TreeElement;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the functionality of TreeElements
 *
 * @author wpride
 */

public class TreeElementTests {

    TreeElement element;
    TreeElement childOne;
    TreeElement childTwo;
    @Before
    public void setup() {
        element = new TreeElement("root");
        childOne = new TreeElement("H2a");
        childTwo = new TreeElement("H3B");

        element.addChild(childOne);
        element.addChild(childTwo);
    }

    @Test
    public void testHashCollision() {
        // childOne and childTwo should have the same hash, but still resolve correctly
        TreeElement getOne = element.getChild("H2a", 0);
        TreeElement getTwo = element.getChild("H3B", 0);
        assertEquals(childOne, getOne);
        assertEquals(childTwo, getTwo);
    }
}
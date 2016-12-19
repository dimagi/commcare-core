package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.TreeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for FormIndex.
 *
 * These tests are not based on any underlying form and use fake tree refs to construct the form
 * indices. This is just meant to test the high-level behavior of form indices themselves.
 *
 * @author Aliza Stone
 */
public class FormIndexTests {

    /**
     * Visualization of the hierarchy created between these indices:
     *
     * 0
     * 1
     *   0
     *   1_1
     *   1_2
     */
    FormIndex index0x1x0x1_1x1_2;
    FormIndex index1x0x1_1x1_2;
    FormIndex index0x1_1x1_2;
    FormIndex index1_1x1_2;
    FormIndex index1_2;

    @Before
    public void setUp() {
        // 1_2
        index1_2 = new FormIndex(1, 2, TreeReference.rootRef());
        // 1_1, 1_2
        index1_1x1_2 = new FormIndex(index1_2, 1, 1, TreeReference.rootRef());
        // 0, 1_1, 1_2
        index0x1_1x1_2 = new FormIndex(index1_1x1_2, 0, TreeReference.rootRef());
        // 1, 0, 1_1, 1_2
        index1x0x1_1x1_2 = new FormIndex(index0x1_1x1_2, 1, TreeReference.rootRef());
        // 0, 1, 0, 1_1, 1_2
        index0x1x0x1_1x1_2 = new FormIndex(index1x0x1_1x1_2, 0, TreeReference.rootRef());
    }

    @Test
    public void testGetNextLevel() {
        FormIndex current = index0x1x0x1_1x1_2;
        Assert.assertEquals(index1x0x1_1x1_2, current = current.getNextLevel());
        Assert.assertEquals(index0x1_1x1_2, current = current.getNextLevel());
        Assert.assertEquals(index1_1x1_2, current = current.getNextLevel());
        Assert.assertEquals(index1_2,  current.getNextLevel());
    }

    @Test
    public void testGetLocalIndex() {
        Assert.assertEquals(1, index1_2.getLocalIndex());
        Assert.assertEquals(0, index0x1_1x1_2.getLocalIndex());
        Assert.assertEquals(1, index1x0x1_1x1_2.getLocalIndex());
    }

    @Test
    public void testGetInstanceIndex() {
        Assert.assertEquals(2, index1_2.getInstanceIndex());
        Assert.assertEquals(1, index1_1x1_2.getInstanceIndex());
        Assert.assertEquals(-1, index0x1_1x1_2.getInstanceIndex());
    }

    @Test
    public void testIsInForm() {
        Assert.assertFalse(FormIndex.createBeginningOfFormIndex().isInForm());
        Assert.assertFalse(FormIndex.createEndOfFormIndex().isInForm());
        Assert.assertTrue(index0x1x0x1_1x1_2.isInForm());
    }

    @Test
    public void testGetLastRepeatInstanceIndex_easyCase() {
        Assert.assertEquals(2, index0x1x0x1_1x1_2.getLastRepeatInstanceIndex());
    }

    @Test
    public void testGetLastRepeatInstanceIndex_notVeryLastIndex() {
        // Add indices to the end of the hierarchy that does NOT have an instance index
        FormIndex index3 = new FormIndex(3, TreeReference.rootRef());
        FormIndex index2x3 = new FormIndex(index3, 2, TreeReference.rootRef());

        index1_2.setNextLevel(index2x3);

        Assert.assertEquals(2, index0x1x0x1_1x1_2.getLastRepeatInstanceIndex());
    }

    @Test
    public void testGetLastRepeatInstanceIndex_nonePresent() {
        // Change to not have a next level for this test
        index0x1_1x1_2.setNextLevel(null);
        Assert.assertEquals(-1, index0x1x0x1_1x1_2.getLastRepeatInstanceIndex());
    }

    @Test
    /**
     * Extended hierarchy created for this test
     *
     * 0
     * 1
     *   0
     *   1_1
     *   1_2
     *      0
     *      1
     *      2_1
     */
    public void testGetLastRepeatInstanceIndex_nestedRepeats() {
        FormIndex extensionIndex2_1 = new FormIndex(2, 1, TreeReference.rootRef());
        FormIndex extensionIndex1x2_1 = new FormIndex(extensionIndex2_1, 1, TreeReference.rootRef());
        FormIndex extensionIndex0x1x2_1 = new FormIndex(extensionIndex1x2_1, 0, TreeReference.rootRef());

        // reset the last index of the original hierarchy to point to the first extension index
        // as its next level
        index1_2.setNextLevel(extensionIndex0x1x2_1);

        Assert.assertEquals(1, index0x1x0x1_1x1_2.getLastRepeatInstanceIndex());
    }
}

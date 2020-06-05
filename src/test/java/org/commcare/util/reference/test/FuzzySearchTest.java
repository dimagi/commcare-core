package org.commcare.util.reference.test;

import org.commcare.cases.util.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author $|-|!˅@M
 */
public class FuzzySearchTest {

    @Test
    public void testFuzzySearch() {
        // Our current implementation allows a maximum difference of 2 between strings to be matched.

        Assert.assertTrue(StringUtils.fuzzyMatch("Rama", "Rmaa").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("Mehrotras", "Mehrotra").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("Clayton", "Cyton").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("Test", "Tasty").first);

        // false since Rama and Rmaa has a difference of 3 characters 'a', 'm' and ','
        Assert.assertFalse(StringUtils.fuzzyMatch("Rama", "Rmaa,").first);

        // true since we check Mehr with Mehrot,
        Assert.assertTrue(StringUtils.fuzzyMatch("Mehr", "Mehrotra").first);

        // false since to check fuzzy search source must have atleast 4 characters.
        Assert.assertFalse(StringUtils.fuzzyMatch("Meh", "Mehrotra").first);

        // false even though we have an exact substring match,
        // Cuz fuzzy search starts checking from 0th location.
        Assert.assertFalse(StringUtils.fuzzyMatch("Test", "CrazyTest").first);

        // Checking for case sensitivity.
        Assert.assertTrue(StringUtils.fuzzyMatch("mehr", "MEHROTRA").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("bugs", "Bugs Bunny").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("BUGS", "Bugs Bunny").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("bugs bunny", "Bugs Bunny").first);
        Assert.assertTrue(StringUtils.fuzzyMatch("BUGS BUNNY", "Bugs Bunny").first);
    }
}

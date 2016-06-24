package org.javarosa.core.util.test;

import org.javarosa.core.util.MathUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.Math;

/**
 * @author ftong
 */
public class MathUtilsTest {
    /**
     * Test that standard and J2ME asins behave similarly.
     * More extensive tests are unnecessary because the code was adapted from
     * http://www.netlib.org/fdlibm/e_asin.c, as cited in the function comments.
     */
    @Test
    public void testAsinsBehaveSimilarly() {
        double tolerance = 0.00000001;
        double standardAsin1 = Math.asin(1);
        double j2meAsin1 = MathUtils.asin(1);
        Assert.assertTrue("Math.asin(1) = " + standardAsin1
                + ", while MathUtils.asin(1) = " + j2meAsin1,
                Math.abs(standardAsin1 - j2meAsin1) < tolerance);
    }
}

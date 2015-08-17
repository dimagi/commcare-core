package org.commcare.test.utils;

import org.junit.Assert;

import org.javarosa.core.io.StreamsUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Helper methods for test evaluation
 *
 * Created by ctsims on 8/14/2015.
 */
public class TestHelpers {
    /**
     * Test that the provided input stream contains the same value as the provided string
     *
     * Fails as a junit assertion if the two do not match.
     */
    public static void assertStreamContentsEqual(InputStream input, String expected) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamsUtil.writeFromInputToOutput(input, baos);
        String result = new String(baos.toByteArray());
        Assert.assertEquals(expected, result);
    }
}

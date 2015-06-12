package org.javarosa.core.util.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.javarosa.core.util.externalizable.ExtWrapIntEncoding;
import org.javarosa.core.util.externalizable.ExtWrapIntEncodingSmall;
import org.javarosa.core.util.externalizable.ExtWrapIntEncodingUniform;

public class NumericEncodingTest extends TestCase {
    public NumericEncodingTest(String name) {
        super(name);
    }

    public NumericEncodingTest() {
        super();
    }

    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new NumericEncodingTest("testIntEncodingUniform"));
        suite.addTest(new NumericEncodingTest("testIntEncodingSmall"));

        return suite;
    }

    public void testNumericEncoding(long val, ExtWrapIntEncoding encoding) {
        ExternalizableTest.testExternalizable(encoding.clone(new Long(val)), encoding, null, this, null);
    }

    public void testIntEncodingUniform() {
        ExtWrapIntEncoding enc = new ExtWrapIntEncodingUniform();

        testNumericEncoding(0, enc);
        testNumericEncoding(-1, enc);
        testNumericEncoding(1, enc);
        testNumericEncoding(-2, enc);

        for (int i = 3; i <= 64; i++) {
            long min = (i < 64 ? -((long)0x01 << (i - 1)) : Long.MIN_VALUE);
            long max = (i < 64 ? ((long)0x01 << (i - 1)) - 1 : Long.MAX_VALUE);

            testNumericEncoding(max - 1, enc);
            testNumericEncoding(max, enc);
            if (i < 64)
                testNumericEncoding(max + 1, enc);
            testNumericEncoding(min + 1, enc);
            testNumericEncoding(min, enc);
            if (i < 64)
                testNumericEncoding(min - 1, enc);
        }
    }

    public void testIntEncodingSmall() {
        int[] biases = {0, 1, 30, 128, 254};
        int[] smallTests = {0, 1, 126, 127, 128, 129, 253, 254, 255, 256, -1, -2, -127, -128, -129};
        int[] largeTests = {3750, -3750, 33947015, -33947015, Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE, Integer.MIN_VALUE + 1};

        for (int i = -1; i < biases.length; i++) {
            int bias;

            if (i == -1) {
                bias = ExtWrapIntEncodingSmall.DEFAULT_BIAS;
            } else {
                bias = biases[i];
                if (bias == ExtWrapIntEncodingSmall.DEFAULT_BIAS)
                    continue;
            }

            ExtWrapIntEncoding enc = new ExtWrapIntEncodingSmall(bias);

            for (int j = 0; j < smallTests.length; j++) {
                testNumericEncoding(smallTests[j], enc);
                if (bias != 0)
                    testNumericEncoding(smallTests[j] - bias, enc);
            }

            for (int j = 0; j < largeTests.length; j++) {
                testNumericEncoding(largeTests[j], enc);
            }
        }
    }
}

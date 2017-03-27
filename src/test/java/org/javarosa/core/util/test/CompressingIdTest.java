package org.javarosa.core.util.test;

import org.javarosa.core.util.CompressingIdGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Clayton Sims (csims@dimagi.com)
 */
public class CompressingIdTest {

    private static final String GROWTH = "HLJXYUWMNV";
    private static final int GB = GROWTH.length();
    private static final String LEAD = "ACE3459KFPRT";
    private static final int LB = LEAD.length();
    private static final String BODY = "ACDEFHJKLMNPQRTUVWXY3479";
    private static final int BB = BODY.length();
    private static final String GROWTH_LIMITED = "123";
    private static final String LEAD_LIMITED = "ABC";
    private static final String BODY_LIMITED = "ABC12345";

    @Test
    public void basicGrowthTests() {
        Assert.assertEquals("AAA", CompressingIdGenerator.generateCompressedIdString(0,
                GROWTH, LEAD, BODY, 2));

        Assert.assertEquals("T99", CompressingIdGenerator.generateCompressedIdString(
                (LB * BB * BB) -1,
                GROWTH, LEAD, BODY, 2));

        Assert.assertEquals("LAAA", CompressingIdGenerator.generateCompressedIdString(
                (LB * BB * BB),
                GROWTH, LEAD, BODY, 2));

        Assert.assertEquals("VT99", CompressingIdGenerator.generateCompressedIdString(
                (GB * LB * BB * BB) - 1,
                GROWTH, LEAD, BODY, 2));

        Assert.assertEquals("LHAAA", CompressingIdGenerator.generateCompressedIdString(
                (GB * LB * BB * BB),
                GROWTH, LEAD, BODY, 2));

        Assert.assertEquals("VVT99", CompressingIdGenerator.generateCompressedIdString(
                (GB * GB * LB * BB * BB)-1,
                GROWTH, LEAD, BODY, 2));
    }

    @Test
    public void noBodyTests() {
        Assert.assertEquals("A", CompressingIdGenerator.generateCompressedIdString(0, GROWTH, LEAD, BODY, 0));
        Assert.assertEquals("T", CompressingIdGenerator.generateCompressedIdString(11, GROWTH, LEAD, BODY, 0));
        Assert.assertEquals("LA", CompressingIdGenerator.generateCompressedIdString(12, GROWTH, LEAD, BODY, 0));
        Assert.assertEquals("VT", CompressingIdGenerator.generateCompressedIdString(119, GROWTH, LEAD, BODY, 0));
    }

    @Test
    public void overlapTests() {
        HashSet<String> generatedIds = new HashSet<>();
        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < 100; ++j) {
                for (int k = 0; k < 100; ++k) {
                    String iPart = CompressingIdGenerator.generateCompressedIdString(i, GROWTH_LIMITED, LEAD_LIMITED, BODY_LIMITED, 1);
                    String jPart = CompressingIdGenerator.generateCompressedIdString(j, GROWTH_LIMITED, LEAD_LIMITED, BODY_LIMITED, 1);
                    String kPart = CompressingIdGenerator.generateCompressedIdString(k, GROWTH_LIMITED, LEAD_LIMITED, BODY_LIMITED, 1);

                    String joined = iPart + jPart + kPart;
                    if (generatedIds.contains(joined)) {
                        Assert.fail(String.format("Duplicate ID [%s]@[%d,%d,%d]", joined, i, j, k));
                    }
                    generatedIds.add(joined);
                }
            }
        }
    }
}

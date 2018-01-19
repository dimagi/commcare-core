package org.javarosa.core.model.test;

import org.javarosa.core.test.FormParseInit;
import org.javarosa.xform.parse.XFormParseException;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Tests for cyclic references
 *
 * @author wpride
 */

public class CyclicReferenceTests {
    /**
     * Test that XPath cyclic reference that references parent throws usable error
     */
    @Test
    public void testCyclicReferenceWithGroup() {
        try {
            new FormParseInit("/xform_tests/group_cyclic_reference.xml");
        } catch (XFormParseException e) {
            String detailMessage = e.getMessage();
            assert detailMessage.contains("Shortest Cycle");
            int newlineCount = detailMessage.length() - detailMessage.replace("\n", "").length();
            assert newlineCount == 3;
            return;
        }
        fail("Cyclical reference did not throw XFormParseException");
    }
}

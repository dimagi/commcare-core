package org.commcare.util.reference.test;

import org.commcare.test.utilities.TestHelpers;
import org.javarosa.core.reference.ReferenceHandler;
import org.javarosa.core.reference.ResourceReference;
import org.javarosa.core.reference.ResourceReferenceFactory;
import org.junit.Assert;

import org.javarosa.core.reference.Reference;
import org.junit.Test;

import java.io.InputStream;

/**
 * Created by ctsims on 8/14/2015.
 */
public class JavaResourceReferenceTest {
    @Test
    public void testReferences() throws Exception {
        ReferenceHandler.instance().addReferenceFactory(new ResourceReferenceFactory());

        String referenceName = "jr://resource/reference/resource_reference_test.txt";

        Reference r = ReferenceHandler.instance().DeriveReference(referenceName);

        if (!(r instanceof ResourceReference)) {
            Assert.fail("Incorrect reference type: " + r);
        }

        Assert.assertEquals(referenceName, r.getURI());

        InputStream stream = r.getStream();

        if (stream == null) {
            Assert.fail("Couldn't find resource at: " + r.getURI());
        }

        TestHelpers.assertStreamContentsEqual(stream, "SUCCESS");
    }

}
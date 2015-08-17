package org.commcare.cases.reference.test;

import org.commcare.test.utils.TestHelpers;
import org.javarosa.core.io.StreamsUtil;
import org.junit.Assert;

import org.commcare.util.reference.JavaResourceReference;
import org.commcare.util.reference.JavaResourceRoot;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

/**
 * Created by ctsims on 8/14/2015.
 */
public class JavaResourceReferenceTest {
    @Test
    public void testReferences() throws Exception{
        ReferenceManager._().addReferenceFactory(new JavaResourceRoot(this.getClass()));

        String referenceName = "jr://resource/reference/resource_reference_test.txt";

        Reference r = ReferenceManager._().DeriveReference(referenceName);

        if(!(r instanceof JavaResourceReference)) {
            Assert.fail("Incorrect reference type: " + r);
        }

        Assert.assertEquals(referenceName, r.getURI());

        InputStream stream = r.getStream();

        if(stream == null) {
            Assert.fail("Couldn't find resource at: " + r.getURI());
        }

        TestHelpers.assertStreamContentsEqual(stream, "SUCCESS");
    }

}
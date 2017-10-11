package org.javarosa.xform.parse;

import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ctsims on 9/27/2017.
 */

public class SubmissionParseTests {

    @Test
    public void minimalParse() {
        Assert.assertNotNull(new FormParseInit("/submission_profiles/submission_minimum.xml").getFormDef().getSubmissionProfile("submitid"));


    }

    @Test
    public void successfulParses() {
        SubmissionProfile profile = new FormParseInit("/submission_profiles/submission_full.xml").getFormDef().getSubmissionProfile("submitid");
        Assert.assertNotNull(profile);

        "http://test.test".equals(profile.getResource());
        XPathReference.getPathExpr("/data/item").equals(profile.getRef());
        XPathReference.getPathExpr("/data/params").equals(profile.getTargetRef());
    }

    @Test(expected = XFormParseException.class)
    public void missingResource() {
        new FormParseInit("/submission_profiles/no_resource.xml");
    }
    @Test(expected = XFormParseException.class)
    public void missingTarget() {
        new FormParseInit("/submission_profiles/missing_target.xml");
    }

    @Test(expected = XFormParseException.class)
    public void invalidTarget() {
        new FormParseInit("/submission_profiles/invalid_target.xml");
    }

    @Test(expected = XFormParseException.class)
    public void invalidSourceRef() {
        new FormParseInit("/submission_profiles/invalid_read_ref.xml");
    }
}

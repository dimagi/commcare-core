package org.javarosa.xform.parse;

import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.model.xform.XPathReference;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ctsims on 9/27/2017.
 */

public class SendParseTests {

    @Test
    public void successfulParses() {
        new FormParseInit("/send_action/succesful_parse.xml");
    }

    @Test(expected = XFormParseException.class)
    public void missingEvent() {
        new FormParseInit("/send_action/missing_event.xml");
    }

    @Test(expected = XFormParseException.class)
    public void missingSubmission() {
        new FormParseInit("/send_action/missing_submission.xml");
    }

}

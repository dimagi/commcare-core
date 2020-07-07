package org.javarosa.xform.parse;

import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.services.locale.Localizer;
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

    @Test
    public void testUnicode() {
        FormParseInit fpi = new FormParseInit("/xform_tests/itext_encoding.xml");
        Localizer l = fpi.getFormDef().getLocalizer();
        Assert.assertEquals("\uD83E\uDDD2", l.getText("four_byte_emoji","en"));
    }

}

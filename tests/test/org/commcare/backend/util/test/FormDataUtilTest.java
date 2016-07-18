package org.commcare.backend.util.test;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionDescriptorUtil;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.FormDataUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class FormDataUtilTest {

    @Test
    public void testOutOfOrderStack() throws Exception {
        MockApp mockApp = new MockApp("/case_title_form_loading/");
        SessionWrapper session = mockApp.getSession();
        UserSandbox sandbox = session.getSandbox();
        SessionWrapper blankSession = new SessionWrapper(session.getPlatform(), sandbox);
        String descriptor = "COMMAND_ID m0 "
                + "COMMAND_ID m3-f0 "
                + "CASE_ID case_id_new_adult_0 case_one "
                + "CASE_ID usercase_id 05c0fb7a77a54eed9872fc1b72a21826 "
                + "CASE_ID return_to m0";
        SessionDescriptorUtil.loadSessionFromDescriptor(descriptor, blankSession);
        String title = FormDataUtil.getTitleFromSession(sandbox, blankSession, blankSession.getEvaluationContext());
        assertEquals("Saul", title);
    }
}

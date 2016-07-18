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

    /**
     * Load form title from session where the case id is computed, not
     * selected, and the case name is loaded from detail referenced by m0-f0,
     * which is implicitly referenced
     */
    @Test
    public void loadRegistrationFormTitleFromSessionTest() throws Exception {
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
        String title = FormDataUtil.getTitleFromSession(sandbox,
                blankSession, blankSession.getEvaluationContext());
        assertEquals("Saul", title);
    }

    /**
     * Load form title from session where the case id is computed, not
     * selected, and the case name is loaded directly since a 'detail' isn't
     * provided
     */
    @Test
    public void loadSimpleRegistrationFormTitleFromSessionTest() throws Exception {
        MockApp mockApp = new MockApp("/case_title_form_loading/");
        SessionWrapper session = mockApp.getSession();
        UserSandbox sandbox = session.getSandbox();
        SessionWrapper blankSession = new SessionWrapper(session.getPlatform(), sandbox);
        String descriptor = "COMMAND_ID m2 "
                + "COMMAND_ID m2-f0 "
                + "CASE_ID case_id_new_adult_0 case_one";
        SessionDescriptorUtil.loadSessionFromDescriptor(descriptor, blankSession);
        String title = FormDataUtil.getTitleFromSession(sandbox,
                blankSession, blankSession.getEvaluationContext());
        assertEquals("Saul", title);
    }

    /**
     * Load form title from session in standard manner, where the case id
     * selected by user
     */
    @Test
    public void loadNormalFormTitleFromSessionTest() throws Exception {
        MockApp mockApp = new MockApp("/case_title_form_loading/");
        SessionWrapper session = mockApp.getSession();
        UserSandbox sandbox = session.getSandbox();
        SessionWrapper blankSession = new SessionWrapper(session.getPlatform(), sandbox);
        String descriptor = "COMMAND_ID m1 "
                + "CASE_ID case_id case_one "
                + "COMMAND_ID m1-f0";
        SessionDescriptorUtil.loadSessionFromDescriptor(descriptor, blankSession);
        String title = FormDataUtil.getTitleFromSession(sandbox,
                blankSession, blankSession.getEvaluationContext());
        assertEquals("Saul", title);
    }
}

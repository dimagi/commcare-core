package org.commcare.backend.session.test;

import org.commcare.core.interfaces.UserSandbox;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionDescriptorUtil;
import org.commcare.session.SessionFrame;
import org.commcare.session.SessionNavigator;
import org.commcare.test.utilities.MockApp;
import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.javarosa.form.api.FormEntryController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by ctsims on 12/2/2016
 */

public class InstanceEvaluationTests {

    private MockApp mApp;
    private MockSessionNavigationResponder mSessionNavigationResponder;
    private SessionNavigator sessionNavigator;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/mixed_instance_initializers/");
        mSessionNavigationResponder = new MockSessionNavigationResponder(mApp.getSession());
        sessionNavigator = new SessionNavigator(mSessionNavigationResponder);
        mApp.getSession().clearVolitiles();
    }

    /**
     * Testing cases where instances are used with different ID's in multiple contexts
     */
    @Test
    public void testMixedInstanceIdCaching() throws Exception {
        SessionWrapper session = mApp.getSession();

        sessionNavigator.startNextSessionStep();

        session.setCommand("m0");

        sessionNavigator.startNextSessionStep();

        session.setCommand("m0-f0");

        sessionNavigator.startNextSessionStep();

        FormEntryController fec = mApp.loadAndInitForm("form_placeholder.xml");

        Assert.assertTrue(fec.getModel().getEvent() == FormEntryController.EVENT_BEGINNING_OF_FORM);

        fec.stepToNextEvent();

        Assert.assertEquals("one",fec.getQuestionPrompts()[0].getQuestionText());
    }
}

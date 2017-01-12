package org.commcare.backend.session.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.SessionNavigator;
import org.commcare.test.utilities.MockApp;
import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.javarosa.form.api.FormEntryController;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by ctsims on 12/2/2016
 */
public class InstanceEvaluationTests {

    private MockApp mApp;
    private SessionNavigator sessionNavigator;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/mixed_instance_initializers/");
        MockSessionNavigationResponder mockSessionNavigationResponder =
                new MockSessionNavigationResponder(mApp.getSession());
        sessionNavigator = new SessionNavigator(mockSessionNavigationResponder);
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

        assertEquals(FormEntryController.EVENT_BEGINNING_OF_FORM, fec.getModel().getEvent());

        fec.stepToNextEvent();

        assertEquals("one", fec.getQuestionPrompts()[0].getQuestionText());
    }

    @Test
    public void loadIndexedInstanceInForm() throws Exception {
        FormEntryController fec = mApp.loadAndInitForm("form_placeholder.xml");

        assertEquals(FormEntryController.EVENT_BEGINNING_OF_FORM, fec.getModel().getEvent());

        fec.stepToNextEvent();
        fec.stepToNextEvent();
        assertEquals("Huffy", fec.getQuestionPrompts()[0].getQuestionText());
    }
}

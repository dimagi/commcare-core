package org.commcare.backend.session.test;

import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.commcare.test.utilities.MockApp;
import org.commcare.session.SessionNavigator;
import org.commcare.util.mocks.SessionWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for SessionNavigator.java
 *
 * @author amstone
 */
public class SessionNavigatorTests {

    private MockApp mApp;
    private MockSessionNavigationResponder mSessionNavigationResponder;
    private SessionNavigator sessionNavigator;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        mSessionNavigationResponder = new MockSessionNavigationResponder(mApp.getSession());
        sessionNavigator = new SessionNavigator(mSessionNavigationResponder);
    }

    private void triggerSessionStepAndCheckResultCode(int expectedResultCode) {
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(expectedResultCode,
                mSessionNavigationResponder.getLastResultCode());
    }

    @Test
    public void testNavWithoutAutoSelect() {
        SessionWrapper session = mApp.getSession();

        // Before anything has been done in the session, the sessionNavigatorResponder should be
        // directed to get a command
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        // Simulate selecting module m0
        session.setCommand("m0");

        // The sessionNavigatorResponder should be prompted to get another command, representing
        // form choice
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        // Simulate selecting a form
        session.setCommand("m0-f1");

        // After a form is chosen for which auto selection is not turned on, the
        // sessionNavigatorResponder should be prompted to start entity selection
        triggerSessionStepAndCheckResultCode(SessionNavigator.START_ENTITY_SELECTION);

        // Simulate going back
        session.stepBack();

        // Confirm that we now need a command again
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        // Simulate selecting the registration form instead
        session.setCommand("m0-f0");

        // The session should need a computed datum (a new case id). However, for computed datums,
        // the session navigator sets it itself; no callout to sessionNavigatorResponder is needed.
        // After setting a computed datum, the session navigator makes a recursive call to
        // startNextSessionStep(), so the sessionNavigatorResponder should now have been prompted
        // to start form entry
        triggerSessionStepAndCheckResultCode(SessionNavigator.START_FORM_ENTRY);
    }

    @Test
    public void testAutoSelectEnabledWithTwoCases() {
        SessionWrapper session = mApp.getSession();

        // Before anything is done in the session, should need a command
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        // Simulate selecting module m0
        session.setCommand("m0");

        // Should now need a form selection
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        // Simulate selecting a form with auto-select enabled
        session.setCommand("m0-f2");

        // Confirm that the next datum has auto-select enabled
        SessionDatum nextNeededDatum = session.getNeededDatum();
        Assert.assertTrue(nextNeededDatum.isAutoSelectEnabled());

        // Since there are 2 cases in the case list (user_restore.xml contains 2 cases of type
        // 'pregnancy', which is the case type that m0-f2's nodeset filters for), the
        // sessionNavigatorResponder will be prompted to start entity select, even though
        // auto-select is enabled
        triggerSessionStepAndCheckResultCode(SessionNavigator.START_ENTITY_SELECTION);
    }


    @Test
    public void testAutoSelectEnabledWithOneCase() {
        SessionWrapper session = mApp.getSession();

        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        session.setCommand("m1");
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);
        session.setCommand("m1-f1");

        // Confirm that the next datum has auto-select enabled and has a confirm detail defined
        SessionDatum nextNeededDatum = session.getNeededDatum();
        Assert.assertTrue(nextNeededDatum.isAutoSelectEnabled());
        Assert.assertNotNull(nextNeededDatum.getLongDetail());

        // Since there is one case in the case list (user_restore.xml contains one case of type
        // 'child', which is the case type that m1-f1's nodeset filters for), and auto-select is
        // enabled, the sessionNavigationResponder should be prompted to launch the confirm detail
        // screen for the auto-selected case
        triggerSessionStepAndCheckResultCode(SessionNavigator.LAUNCH_CONFIRM_DETAIL);
    }

    @Test
    public void testAutoSelectEnabledWithOneCaseAndNoDetail() {
        SessionWrapper session = mApp.getSession();

        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);

        session.setCommand("m1");
        triggerSessionStepAndCheckResultCode(SessionNavigator.GET_COMMAND);
        session.setCommand("m1-f2");

        // Confirm that the next datum has auto-select enabled, but does NOT have a confirm detail
        // defined
        SessionDatum nextNeededDatum = session.getNeededDatum();
        Assert.assertTrue(nextNeededDatum.isAutoSelectEnabled());
        Assert.assertNull(nextNeededDatum.getLongDetail());

        // Since there is one case in the case list and auto-select is enabled, but there is no
        // confirm detail screen, the sessionNavigationResponder should be prompted to go directly
        // to form entry
        triggerSessionStepAndCheckResultCode(SessionNavigator.START_FORM_ENTRY);
    }


}

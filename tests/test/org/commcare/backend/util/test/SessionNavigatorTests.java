package org.commcare.backend.util.test;

import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.SessionNavigator;
import org.commcare.util.mocks.SessionWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for SessionNavigator.java
 */
public class SessionNavigatorTests {

    MockApp mApp;
    MockSessionNavigationResponder mSessionNavigationResponder;
    SessionNavigator sessionNavigator;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        mSessionNavigationResponder = new MockSessionNavigationResponder(mApp.getSession());
        sessionNavigator = new SessionNavigator(mSessionNavigationResponder);
    }

    @Test
    public void testNavWithoutAutoSelect() {
        SessionWrapper session = mApp.getSession();

        // Before anything is done in the session, should need a command
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        // Simulate selecting module m0
        session.setCommand("m0");

        // The sessionNavigatorResponder should be prompted to get another command, representing
        // form choice
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        // Simulate selecting a form
        session.setCommand("m0-f1");

        // After a form is chosen for which auto selection is not turned on, the
        // sessionNavigatorResponder should be prompted to start entity selection
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.START_ENTITY_SELECTION,
                mSessionNavigationResponder.getLastResultCode());

        // Simulate going back
        session.stepBack();
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        // Simulate selecting the registration form instead
        session.setCommand("m0-f0");

        // The session should need a computed datum (a new case id). However, for computed datums,
        // the session navigator sets it itself; no callout to sessionNavigatorResponder is needed
        sessionNavigator.startNextSessionStep();

        // After setting a computed datum, the session navigator makes a recursive call to
        // startNextSessionStep(), so the sessionNavigatorResponder should now have been prompted
        // to start form entry
        Assert.assertEquals(SessionNavigator.START_FORM_ENTRY,
                mSessionNavigationResponder.getLastResultCode());

    }

    @Test
    public void testNavWithAutoSelectEnabledAndZeroCases() {
        SessionWrapper session = mApp.getSession();

        // Before anything is done in the session, should need a command
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        // Simulate selecting module m1
        session.setCommand("m1");

        // The sessionNavigatorResponder should be prompted to get another command, representing
        // form choice
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        // Simulate selecting a form (which has auto-select enabled)
        session.setCommand("m1-f1");

        // Confirm that the next datum has auto-select enabled
        SessionDatum nextNeededDatum = session.getNeededDatum();
        Assert.assertTrue(nextNeededDatum.isAutoSelectEnabled());

        // Since there are 0 cases in the case list, the sessionNavigatorResponder should be
        // prompted to start entity selection, even though auto-select is on
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.START_ENTITY_SELECTION,
                mSessionNavigationResponder.getLastResultCode());

    }

    @Test
    public void testNavWithAutoSelectEnabledAndOneCase() {
        SessionWrapper session = mApp.getSession();

        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        session.setCommand("m1");
        sessionNavigator.startNextSessionStep();
        Assert.assertEquals(SessionNavigator.GET_COMMAND,
                mSessionNavigationResponder.getLastResultCode());

        session.setCommand("m1-f1");

        // Confirm that the next datum has auto-select enabled and has a confirm detail defined
        SessionDatum nextNeededDatum = session.getNeededDatum();
        Assert.assertTrue(nextNeededDatum.isAutoSelectEnabled());
        Assert.assertNotNull(nextNeededDatum.getLongDetail());

        // Since there is one case in the case list and auto-select is enabled, the
        // sessionNavigationResponder should be prompted to launch the confirm detail screen for
        // the auto-selected case
        
    }


}

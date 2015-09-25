package org.commcare.backend.util.test;

import org.commcare.test.utilities.MockSessionNavigationResponder;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.SessionNavigationResponder;
import org.commcare.util.SessionNavigator;
import org.junit.Before;

/**
 * Created by amstone326 on 9/25/15.
 */
public class SessionNavigatorTests {

    MockApp mApp;
    SessionNavigationResponder mSessionNavigationResponder;
    SessionNavigator sessionNavigator;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        mSessionNavigationResponder = new MockSessionNavigationResponder(mApp.getSession());
        sessionNavigator = new SessionNavigator(mSessionNavigationResponder);
    }


}

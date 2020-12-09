package org.commcare.backend.suite.model.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.Global;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.suite.model.MenuLoader;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.LoggerInterface;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for basic app models for case claim
 *
 * @author ctsims
 */
public class CaseClaimModelTests {

    @Test
    public void testRemoteQueryDatum() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        SessionDatum datum = session.getNeededDatum();

        Assert.assertTrue("Didn't find Remote Query datum definition", datum instanceof RemoteQueryDatum);
    }
}

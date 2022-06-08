package org.commcare.backend.suite.model.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.mocks.MockQueryClient;
import org.commcare.util.screen.QueryScreen;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

/**
 * Basic test for remote query as part of form entry session
 */
public class QueryModelTests {

    @Test
    public void testQueryEntryDatum() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("m0-f0");

        SessionDatum datum = session.getNeededDatum();
        Assert.assertTrue(datum instanceof RemoteQueryDatum);
        Assert.assertEquals("registry1", datum.getDataId());

        // construct the screen
        QueryScreen screen = new QueryScreen("username", "password", System.out);
        screen.init(session);

        // mock the query response
        InputStream response = this.getClass().getResourceAsStream("/case_claim_example/query_response.xml");
        screen.setClient(new MockQueryClient(response));

        // perform the query
        boolean success = screen.handleInputAndUpdateSession(session, "", false, null);
        Assert.assertTrue(success);

        // check that session datum requirement is satisfied
        assert session.getNeededDatum() == null;
    }
}

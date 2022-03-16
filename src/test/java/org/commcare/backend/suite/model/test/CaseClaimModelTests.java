package org.commcare.backend.suite.model.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.MockApp;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        RemoteQuerySessionManager remoteQuerySessionManager = RemoteQuerySessionManager.buildQuerySessionManager(
                session, session.getEvaluationContext(), new ArrayList<>());

        remoteQuerySessionManager.answerUserPrompt("patient_id", "123");
        Multimap<String, String> rawQueryParams = remoteQuerySessionManager.getRawQueryParams(true);

        List<String> expected = ImmutableList.of("123");
        Assert.assertEquals(expected, rawQueryParams.get("patient_id_from_input"));
    }
}

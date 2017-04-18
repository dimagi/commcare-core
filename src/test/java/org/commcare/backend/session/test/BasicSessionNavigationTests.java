package org.commcare.backend.session.test;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.test.utilities.MockApp;
import org.commcare.session.SessionFrame;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests navigating through a CommCareSession (setting datum values and commands, using stepBack(),
 * etc.) for a sample app
 *
 * @author amstone
 */
public class BasicSessionNavigationTests {

    private MockApp mApp;
    private SessionWrapper session;

    @Before
    public void setUp() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        session = mApp.getSession();
    }

    @Test
    public void testNeedsCommandFirst() {
        // Before anything is done in the session, should need a command
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        // After setting first command to m0, should still need another command because the 3 forms
        // within m0 have different datum needs, so will prioritize getting the next command first
        session.setCommand("m0");
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        // After choosing the form, will need a case id
        session.setCommand("m0-f1");
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());

        // Stepping back after choosing a command should go back only 1 level
        session.stepBack();
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        Assert.assertEquals("m0", session.getCommand());

        // After choosing registration form, should need computed case id
        session.setCommand("m0-f0");
        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        session.setComputedDatum();
        Assert.assertEquals(null, session.getNeededData());
    }

    @Test
    public void testNeedsCaseFirst() {
        // Before anything is done in the session, should need a command
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        // After setting first command to m2, should need a case id, because both forms within m2 need one
        session.setCommand("m2");
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());

        // After setting case id, should need to choose a form
        session.setDatum("case_id", "case_two");
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        // Should be ready to go after choosing a form
        session.setCommand("m2-f1");
        Assert.assertEquals(null, session.getNeededData());
    }

    @Test
    public void testStepBackBasic() {
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m1");
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m1-f3");
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
        session.setDatum("case_id", "case_one");
        Assert.assertEquals(null, session.getNeededDatum());

        // Should result in needing a case_id again
        session.stepBack();
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
    }

    @Test
    public void testStepBackWithExtraValue() {
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m1");
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m1-f3");
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
        session.setDatum("case_id", "case_one");
        Assert.assertEquals(null, session.getNeededDatum());
        session.setDatum("return_to", "m1");

        // Should pop 2 values off of the session stack in order to return to the last place
        // where there was a user-inputted decision
        session.stepBack();
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
    }

    @Test
    public void testStepBackWithComputedDatum() {
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f0");
        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());
        session.setComputedDatum();
        Assert.assertEquals(null, session.getNeededData());

        // Should pop 2 values off of the session stack so that the next needed data isn't a
        // computed value
        session.stepBack();
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
    }

    @Test
    public void testStepToSyncRequest() {
        session.setCommand("patient-case-search");
        Assert.assertEquals(SessionFrame.STATE_QUERY_REQUEST, session.getNeededData());

        ExternalDataInstance dataInstance =
                SessionStackTests.buildRemoteExternalDataInstance(this.getClass(),
                        session, "/session-tests-template/patient_query_result.xml");
        session.setQueryDatum(dataInstance);

        // case_id
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
        session.setDatum("case_id", "123");

        // time to make sync request
        Assert.assertEquals(SessionFrame.STATE_SYNC_REQUEST, session.getNeededData());
    }

    /**
     * Try selecting case already in local case db
     */
    @Test
    public void testStepToIrrelevantSyncRequest() {
        session.setCommand("patient-case-search");
        Assert.assertEquals(SessionFrame.STATE_QUERY_REQUEST, session.getNeededData());

        ExternalDataInstance dataInstance =
                SessionStackTests.buildRemoteExternalDataInstance(this.getClass(),
                        session, "/session-tests-template/patient_query_result.xml");
        session.setQueryDatum(dataInstance);

        // case_id
        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());
        // select case present in user_restore
        session.setDatum("case_id", "case_one");

        // assert that relevancy condition of post request is false
        Assert.assertEquals(null, session.getNeededData());
    }

    @Test
    public void testInvokeEmptySyncRequest() {
        SessionWrapper session = mApp.getSession();

        session.setCommand("empty-remote-request");
        Assert.assertEquals(SessionFrame.STATE_SYNC_REQUEST, session.getNeededData());
    }

    @Test
    public void testStepToSyncRequestRelevancy() {
        session.setCommand("irrelevant-remote-request");
        Assert.assertEquals(null, session.getNeededData());

        session.setCommand("relevant-remote-request");
        Assert.assertEquals(SessionFrame.STATE_SYNC_REQUEST, session.getNeededData());
    }
}

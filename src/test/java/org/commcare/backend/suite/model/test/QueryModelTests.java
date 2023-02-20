package org.commcare.backend.suite.model.test;

import com.google.common.collect.ImmutableMap;

import org.cli.MockSessionUtils;
import org.commcare.core.encryption.CryptUtil;
import org.commcare.core.interfaces.MemoryVirtualDataInstanceStorage;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.screen.CommCareSessionException;
import org.commcare.util.screen.QueryScreen;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.InstanceRoot;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

/**
 * Basic test for remote query as part of form entry session
 */
public class QueryModelTests {

    MemoryVirtualDataInstanceStorage virtualDataInstanceStorage = new MemoryVirtualDataInstanceStorage();

    @Before
    public void setUp() {
        virtualDataInstanceStorage.clear();
    }

    @Test
    public void testQueryEntryDatum() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");
        SessionWrapper session = mApp.getSession();
        QueryScreen screen = setupQueryScreen(session);

        // perform the query
        boolean success = screen.handleInputAndUpdateSession(session, "bob,23", false, null);
        Assert.assertTrue(success);

        // check that session datum requirement is satisfied
        Assert.assertNull(session.getNeededDatum());
    }

    @Test
    public void testScreenCreatesVirtualInstance() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");
        SessionWrapper session = mApp.getSession();
        QueryScreen screen = setupQueryScreen(session);

        String refId = "registry1";
        String instanceID = VirtualInstances.makeSearchInputInstanceID(refId);
        String expectedInstanceStorageKey = CryptUtil.sha256(instanceID + "/name=bob|age=23|");
        Assert.assertFalse(virtualDataInstanceStorage.contains(expectedInstanceStorageKey));

        // perform the query
        boolean success = screen.handleInputAndUpdateSession(session, "bob,23", false, null);
        Assert.assertTrue(success);

        // check that saved instance matches expect what we expect
        Assert.assertTrue(virtualDataInstanceStorage.contains(expectedInstanceStorageKey));
        Map<String, String> input = ImmutableMap.of("name", "bob", "age", "23");
        Assert.assertEquals(
                VirtualInstances.buildSearchInputInstance(refId, input).getRoot(),
                virtualDataInstanceStorage.read(expectedInstanceStorageKey, instanceID, refId).getRoot());

        CaseTestUtils.xpathEvalAndAssert(
                session.getEvaluationContext(),
                "instance('search-input:registry1')/input/field[@name='age']",
                "23");

        // test loading instance with new ref
        ExternalDataInstance instance = new ExternalDataInstance("jr://instance/search-input/registry1",
                "custom-id");
        Assert.assertNotNull(session.getIIF().generateRoot(instance).getRoot());

        // test that we can still load instances using the legacy ref
        ExternalDataInstance legacyInstance = new ExternalDataInstance("jr://instance/search-input",
                "search-input:registry1");
        Assert.assertNotNull(session.getIIF().generateRoot(legacyInstance).getRoot());
    }

    @NotNull
    private QueryScreen setupQueryScreen(SessionWrapper session) throws CommCareSessionException {
        session.setCommand("m0-f0");

        SessionDatum datum = session.getNeededDatum();
        Assert.assertTrue(datum instanceof RemoteQueryDatum);
        Assert.assertEquals("registry1", datum.getDataId());

        // construct the screen
        // mock the query response
        InputStream response = this.getClass().getResourceAsStream("/case_claim_example/query_response.xml");
        QueryScreen screen = new QueryScreen(
                "username", "password",
                System.out, virtualDataInstanceStorage, new MockSessionUtils(response));
        screen.init(session);
        return screen;
    }
}

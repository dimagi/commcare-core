package org.commcare.backend.session.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.CommCareSession;
import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Tests for assertions in menus
 */
public class MenuTests {

    MockApp appWithMenuAssertions;
    Suite suite;

    @Before
    public void setup() throws Exception {
        appWithMenuAssertions = new MockApp("/app_structure/");
        suite = appWithMenuAssertions.getSession().getPlatform().getInstalledSuites().get(0);
    }


    @Test
    public void testAssertionsEvaluated() throws Exception {
        Menu menuWithAssertionsBlock = suite.getMenusWithId("m0").get(0);
        AssertionSet assertions = menuWithAssertionsBlock.getAssertions();
        EvaluationContext ec = appWithMenuAssertions.getSession().getEvaluationContext();
        Text assertionFailures = assertions.getAssertionFailure(ec);
        assertNotNull(assertions);
        assertEquals("custom_assertion.m0.0", assertionFailures.getArgument());
    }

    /**
     * When there are multiple menu blocks with same ids, we should accumulate the required instances from all
     * of the menus and their contained entries
     */
    @Test
    public void testMenuInstances_WhenMenuHaveSameIds() {
        SessionWrapper currentSession = appWithMenuAssertions.getSession();
        EvaluationContext ec = currentSession.getEvaluationContext(currentSession.getIIF(), "m3", null);
        List<String> instanceIds = ec.getInstanceIds();
        assertEquals(2, instanceIds.size());
        assertTrue(instanceIds.contains("my_instance"));
        assertTrue(instanceIds.contains("casedb"));
    }
}

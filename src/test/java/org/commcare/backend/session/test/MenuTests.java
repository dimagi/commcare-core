package org.commcare.backend.session.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.commcare.suite.model.AssertionSet;
import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Before;
import org.junit.Test;


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
}

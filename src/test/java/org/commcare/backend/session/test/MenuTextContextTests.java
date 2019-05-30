package org.commcare.backend.session.test;

import org.commcare.suite.model.Menu;
import org.commcare.suite.model.Suite;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.analysis.InstanceNameAccumulatingAnalyzer;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by amstone326 on 8/14/17.
 */

public class MenuTextContextTests {

    MockApp appWithMenuDisplayConditions;
    Suite suite;

    @Before
    public void setup() throws Exception {
        appWithMenuDisplayConditions = new MockApp("/app_with_dynamic_menu_text_and_context/");
        suite = appWithMenuDisplayConditions.getSession().getPlatform().getInstalledSuites().get(0);
    }

    @Test
    public void testRestrictedEvalContextGeneration1() throws Exception {
        //
    }
}

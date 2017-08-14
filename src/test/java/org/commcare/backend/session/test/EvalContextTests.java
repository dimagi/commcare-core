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

public class EvalContextTests {

    MockApp appWithMenuDisplayConditions;
    Suite suite;

    @Before
    public void setup() throws Exception {
        appWithMenuDisplayConditions = new MockApp("/app_with_menu_display_conditions/");
        suite = appWithMenuDisplayConditions.getSession().getPlatform().getInstalledSuites().get(0);
    }

    @Test
    public void testRestrictedEvalContextGeneration1() throws Exception {
        // Get the "restricted" eval context for m0
        Menu m0 = suite.getMenusWithId("m0").get(0);
        XPathExpression relevancyCondition = m0.getMenuRelevance();
        Set<String> instancesNeededByRelevancyCondition =
                (new InstanceNameAccumulatingAnalyzer()).accumulateAsSet(relevancyCondition);

        // Get the eval context for a command ID that has 3 instances in scope, but restrict it
        // to just those needed by the relevancy condition (only "casedb" in this case)
        EvaluationContext ec = appWithMenuDisplayConditions.getSession().
                getRestrictedEvaluationContext(m0.getId(), instancesNeededByRelevancyCondition);

        // 1) Confirm that the eval context was restricted properly
        List<String> instancesThatShouldBeIncluded = new ArrayList<>();
        instancesThatShouldBeIncluded.add("casedb");
        Assert.assertEquals(instancesThatShouldBeIncluded, ec.getInstanceIds());

        // 2) Confirm the display condition was evaluated properly with the restricted context
        Assert.assertTrue(FunctionUtils.toBoolean(relevancyCondition.eval(ec)));
    }

    @Test
    public void testRestrictedEvalContextGeneration2() throws Exception {
        // Get the "restricted" eval context for m1
        Menu m1 = suite.getMenusWithId("m1").get(0);
        XPathExpression relevancyCondition = m1.getMenuRelevance();
        Set<String> instancesNeededByRelevancyCondition =
                (new InstanceNameAccumulatingAnalyzer()).accumulateAsSet(relevancyCondition);

        // Get the eval context for a command ID that has 3 instances in scope, but restrict it
        // to just those needed by the relevancy condition (only "commcaresession" in this case)
        EvaluationContext ec = appWithMenuDisplayConditions.getSession().
                getRestrictedEvaluationContext(m1.getId(), instancesNeededByRelevancyCondition);

        // 1) Confirm that the eval context was restricted properly
        List<String> instancesThatShouldBeIncluded = new ArrayList<>();
        instancesThatShouldBeIncluded.add("commcaresession");
        Assert.assertEquals(instancesThatShouldBeIncluded, ec.getInstanceIds());

        // 2) Confirm the display condition was evaluated properly with the restricted context
        Assert.assertFalse(FunctionUtils.toBoolean(relevancyCondition.eval(ec)));
    }
}

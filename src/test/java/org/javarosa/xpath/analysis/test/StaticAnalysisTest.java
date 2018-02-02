package org.javarosa.xpath.analysis.test;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.ContainsUncacheableExpressionAnalyzer;
import org.javarosa.xpath.analysis.ReferencesMainInstanceAnalyzer;
import org.javarosa.xpath.analysis.InstanceNameAccumulatingAnalyzer;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for the XPath static analysis infrastructure
 *
 * @author Aliza Stone
 */
public class StaticAnalysisTest {

    private static String NO_INSTANCES_EXPR =
            "double(now()) > (double(/data/last_viewed) + 10)";
    private static String ONE_INSTANCE_EXPR =
            "instance('casedb')/casedb/case[@case_type='case'][@status='open']";
    private static String DUPLICATED_INSTANCE_EXPR =
            "count(instance('commcaresession')/session/user/data/role) > 0 and " +
                    "instance('commcaresession')/session/user/data/role= 'case_manager'";
    private static String EXPR_WITH_INSTANCE_IN_PREDICATE =
            "instance('casedb')/casedb/case[@case_type='commcare-user']" +
                    "[hq_user_id=instance('commcaresession')/session/context/userid]/@case_id";
    private static String RIDICULOUS_RELEVANCY_CONDITION_FROM_REAL_APP =
            "(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/current_schedule_phase = 2 " +
                    "and instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add != '' and " +
                    "(today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) " +
                    "+ int(instance('schedule:m5:p2:f2')/schedule/@starts)) and (instance('schedule:m5:p2:f2')/schedule/@expires = '' " +
                    "or today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) + " +
                    "int(instance('schedule:m5:p2:f2')/schedule/@expires))))) and " +
                    "(instance('schedule:m5:p2:f2')/schedule/@allow_unscheduled = 'True' or " +
                    "count(instance('schedule:m5:p2:f2')/schedule/visit[instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_number_cf = '' " +
                    "or if(@repeats = 'True', @id >= instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_number_cf, " +
                    "@id > instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_number_cf)]" +
                    "[if(@repeats = 'True', today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_date_cf) + " +
                    "int(@increment) + int(@starts)) and (@expires = '' or today() <= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_date_cf) + " +
                    "int(@increment) + int(@expires))), today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) + " +
                    "int(@due) + int(@starts)) and (@expires = '' or today() <= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) + " +
                    "int(@due) + int(@expires))))]) > 0)";

    private static String BASE_CONTEXT_REF_aCase = "instance('casedb')/casedb/case[651]";
    private static String BASE_CONTEXT_REF_aNode = "instance('baseinstance')/base/element";

    private static String BASIC_RELATIVE_EXPR = "./@case_name";
    private static String EXPR_WITH_CURRENT_AT_TOP_LEVEL =
            "(instance('adherence:calendar')/calendar/year/month/day[@date > (today()-36) and " +
                    "@date < (today()-28) and @name='Sunday']/@date) = current()/date_registered";
    private static String EXPR_WITH_CURRENT_IN_PREDICATE =
            "if(instance('casedb')/casedb/case[@case_id=current()/index/parent]/date_hh_registration = '', '', " +
                    "format_date(date(instance('casedb')/casedb/case[@case_id=current()/index/parent]/date_hh_registration),'short'))";
    private static String RELATIVE_EXPR_WITH_PREDICATE =
            "../element[@id=instance('commcaresession')/session/data/case_id_loaded]";

    @Test
    public void testInstanceAccumulatingAnalyzer() throws XPathSyntaxException {
        testInstanceAccumulate(NO_INSTANCES_EXPR,
                new String[]{});
        testInstanceAccumulate(ONE_INSTANCE_EXPR,
                new String[]{"casedb"});
        testInstanceAccumulate(DUPLICATED_INSTANCE_EXPR,
                new String[]{"commcaresession"});
        testInstanceAccumulate(EXPR_WITH_INSTANCE_IN_PREDICATE,
                new String[]{"casedb", "commcaresession"});
        testInstanceAccumulate(RIDICULOUS_RELEVANCY_CONDITION_FROM_REAL_APP,
                new String[]{"casedb", "commcaresession", "schedule:m5:p2:f2"});

        // Test the length of the result with list accumulation, just to ensure it gets them all
        List<String> parsedInstancesList =
                (new InstanceNameAccumulatingAnalyzer()).accumulateAsList(
                        XPathParseTool.parseXPath(RIDICULOUS_RELEVANCY_CONDITION_FROM_REAL_APP));
        assertEquals(27, parsedInstancesList.size());
    }

    @Test
    public void testCurrentAndRelativeRefs() throws XPathSyntaxException {
        testInstanceAccumulate(BASIC_RELATIVE_EXPR, new String[]{"casedb"},
                BASE_CONTEXT_REF_aCase);
        testInstanceAccumulate(EXPR_WITH_CURRENT_AT_TOP_LEVEL, new String[]{"adherence:calendar", "casedb"},
                BASE_CONTEXT_REF_aCase);

        // expect null because no context ref was provided when it was needed
        testInstanceAccumulate(BASIC_RELATIVE_EXPR, null);
        testInstanceAccumulate(EXPR_WITH_CURRENT_AT_TOP_LEVEL, null);

        // should be OK not to provide a base context ref here because current() is only being
        // used within a predicate, so it should use the sub-context
        testInstanceAccumulate(EXPR_WITH_CURRENT_IN_PREDICATE, new String[]{"casedb"});

        // This analysis should fail because no context ref was provided
        testInstanceAccumulate(RELATIVE_EXPR_WITH_PREDICATE, null);

        testInstanceAccumulate(RELATIVE_EXPR_WITH_PREDICATE,
                new String[]{"commcaresession", "baseinstance"}, BASE_CONTEXT_REF_aNode);
    }

    private void testInstanceAccumulate(String expressionString, String[] expectedInstances)
            throws XPathSyntaxException {
        testInstanceAccumulate(expressionString, expectedInstances, null);
    }

    private void testInstanceAccumulate(String expressionString, String[] expectedInstances,
                                      String baseContextString)
            throws XPathSyntaxException {

        InstanceNameAccumulatingAnalyzer analyzer;

        if (baseContextString != null) {
            TreeReference baseContextRef =
                    ((XPathPathExpr)XPathParseTool.parseXPath(baseContextString)).getReference();
            analyzer = new InstanceNameAccumulatingAnalyzer(baseContextRef);
        } else {
            analyzer = new InstanceNameAccumulatingAnalyzer();
        }

        Set<String> expectedInstancesSet = null;
        if (expectedInstances != null) {
            expectedInstancesSet = new HashSet<>();
            for (String s : expectedInstances) {
                expectedInstancesSet.add(s);
            }
        }

        Set<String> parsedInstancesSet =
                analyzer.accumulate(XPathParseTool.parseXPath(expressionString));
        assertEquals(expectedInstancesSet, parsedInstancesSet);
    }

    @Test
    public void testReferencesMainInstanceAnalysis() throws XPathSyntaxException {
        testReferencesMainInstance("/unicorn/color[@name='fred']",
                "unicorn", true);
        testReferencesMainInstance("date(/data/refill/next_refill_due_date)",
                "data", true);

        String longExpressionWithMainInstanceRef =
                "instance('adherence_schedules')/adherence_schedules_list/adherence_schedules[" +
                        "id = /data/schedule_id][/data/user/user_level = 'dev' or user_level = 'real']/doses_per_week";
        testReferencesMainInstance(longExpressionWithMainInstanceRef, "data", true);

        String evenLongerExpressionWithMainInstanceRef =
                "date(coalesce(instance('casedb')/casedb/case[@case_id = instance('commcaresession')" +
                        "/session/blah/case_id_load_episode_case]/refill_next_date, " +
                        "(date(coalesce(instance('casedb')/casedb/case[@case_id = " +
                        "instance('commcaresession')/session/blah/case_id_load_episode_case]/adherence_schedule_date_start, " +
                        "/data/treatment_initiation_date)) + 30)))";
        testReferencesMainInstance(evenLongerExpressionWithMainInstanceRef, "data", true);

        testReferencesMainInstance("/unicorn/color[@name='fred']",
                "color", false);
        testReferencesMainInstance("instance('commcaresession')/session/data/case_id_load_test",
                "data", false);

        String longExpressionWithoutMainInstanceRef =
                "date(coalesce(instance('casedb')/casedb/case[@case_id = instance('commcaresession')" +
                        "/session/data/case_id_load_episode_case]/refill_next_date, " +
                        "(date(coalesce(instance('casedb')/casedb/case[@case_id = " +
                        "instance('commcaresession')/session/data/case_id_load_episode_case]/adherence_schedule_date_start, " +
                        "/blah/treatment_initiation_date)) + 30)))";
        testReferencesMainInstance(longExpressionWithoutMainInstanceRef, "data", false);
    }

    private void testReferencesMainInstance(String expressionString, String instanceName, boolean expectedResult) throws XPathSyntaxException {
        ReferencesMainInstanceAnalyzer analyzer = new ReferencesMainInstanceAnalyzer(instanceName);
        try {
            assertEquals(expectedResult, analyzer.computeResult(XPathParseTool.parseXPath(expressionString)));
        } catch (AnalysisInvalidException e) {
            fail("Encountered Analysis Invalid exception: " + e.getMessage());
        }
    }

    @Test
    public void testContainsUncacheableExpressionAnalysis() throws XPathSyntaxException {
        testContainsUncacheable("now()", true);
        testContainsUncacheable("uuid()", true);
        testContainsUncacheable("random()", true);
        testContainsUncacheable("depend(/data/val1, /data/val2)", true);
        testContainsUncacheable("sleep(1000, -1)", true);
        testContainsUncacheable("date(/data/refill/next_refill_due_date) <= today()", true);
        testContainsUncacheable(
                "concat(format-date(today(), '%e/%n/%y'), ': ', /data/ql_weight_and_height/weight, ' ', jr:itext('localization/kg-label'))",
                true);
        testContainsUncacheable("/data/val1", false);
    }

    private void testContainsUncacheable(String expressionString, boolean expectedResult) throws XPathSyntaxException {
        ContainsUncacheableExpressionAnalyzer analyzer = new ContainsUncacheableExpressionAnalyzer();
        try {
            assertEquals(expectedResult, analyzer.computeResult(XPathParseTool.parseXPath(expressionString)));
        } catch (AnalysisInvalidException e) {
            fail("Encountered Analysis Invalid exception: " + e.getMessage());
        }
    }
}

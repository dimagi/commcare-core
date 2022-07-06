package org.commcare.backend.suite.model.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import org.commcare.data.xml.SimpleNode;
import org.commcare.data.xml.TreeBuilder;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.modern.session.SessionWrapper;
import org.commcare.session.RemoteQuerySessionManager;
import org.commcare.suite.model.QueryPrompt;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.SessionDatum;
import org.commcare.test.utilities.MockApp;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

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
    public void testPopulateItemsetChoices__inputReference() throws Exception {
        RemoteQuerySessionManager remoteQuerySessionManager = testPopulateItemsetChoices(
                ImmutableMap.of("state", "ka"), ImmutableList.of("bang"), null);

        // test updating input updates the dependent itemset
        testPopulateItemsetChoices(
                ImmutableMap.of("state", "rj"), ImmutableList.of("kota"), remoteQuerySessionManager);
    }

    @Test
    public void testPopulateItemsetChoices__emptyInput() throws Exception {
        testPopulateItemsetChoices(Collections.emptyMap(), Collections.emptyList(), null);
    }

    private RemoteQuerySessionManager testPopulateItemsetChoices(Map<String, String> userInput, List<String> expected,
            RemoteQuerySessionManager existingQuerySessionManager)
            throws Exception {
        RemoteQuerySessionManager remoteQuerySessionManager =
                existingQuerySessionManager == null ? buildRemoteQuerySessionManager()
                        : existingQuerySessionManager;

        userInput.forEach(remoteQuerySessionManager::answerUserPrompt);

        OrderedHashtable<String, QueryPrompt> inputDisplays =
                remoteQuerySessionManager.getNeededUserInputDisplays();
        QueryPrompt districtPrompt = inputDisplays.get("district");

        remoteQuerySessionManager.populateItemSetChoices(districtPrompt);
        List<String> choices = districtPrompt.getItemsetBinding().getChoices().stream().map(
                SelectChoice::getValue).collect(Collectors.toList());
        Assert.assertEquals(expected, choices);
        return remoteQuerySessionManager;
    }

    private RemoteQuerySessionManager buildRemoteQuerySessionManager() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        ExternalDataInstance districtInstance = buildDistrictInstance();
        ExternalDataInstance stateInstance = buildStateInstance();
        EvaluationContext context = session.getEvaluationContext().spawnWithCleanLifecycle(ImmutableMap.of(
                stateInstance.getInstanceId(), stateInstance,
                districtInstance.getInstanceId(), districtInstance
        ));

        return RemoteQuerySessionManager.buildQuerySessionManager(
                session, context, ImmutableList.of(QueryPrompt.INPUT_TYPE_SELECT1));
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput() throws Exception {
        RemoteQuerySessionManager remoteQuerySessionManager = testGetRawQueryParamsWithUserInput(
                ImmutableMap.of("patient_id", "123"),
                ImmutableList.of("external_id = 123"),
                "patient_id"
        );

        // test that updating the input results in an updated output
        testGetRawQueryParamsWithUserInput(
                ImmutableMap.of("patient_id", "124"),
                ImmutableList.of("external_id = 124"),
                "patient_id",
                remoteQuerySessionManager
        );


    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput_missing() throws Exception {
        testGetRawQueryParamsWithUserInput(Collections.emptyMap(), ImmutableList.of(""), "patient_id");
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput_legacy() throws Exception {
        testGetRawQueryParamsWithUserInput(
                ImmutableMap.of("patient_id", "123"),
                ImmutableList.of("external_id = 123"),
                "patient_id_legacy"
        );
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput_missing_legacy() throws Exception {
        testGetRawQueryParamsWithUserInput(Collections.emptyMap(), ImmutableList.of(""), "patient_id_legacy");
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput_customInstanceId()
            throws Exception {
        testGetRawQueryParamsWithUserInput(
                ImmutableMap.of("patient_id", "123"),
                ImmutableList.of("external_id = 123"),
                "patient_id_custom_id"
        );
    }

    /**
     * Test that using 'current()' works with the lazy initialized instances
     */
    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput_current() throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        Map<String, String> input = ImmutableMap.of("name", "bob", "age", "23");
        ExternalDataInstance userInputInstance = VirtualInstances.buildSearchInputInstance("patients", input);

        // make sure the evaluation context doesn't get an instance with ID=userInputInstance.instanceID
        // After this there should this instance should be registered under 2 IDs: 'bad-id' and 'my-search-input'
        ImmutableMap<String, ExternalDataInstance> instances = ImmutableMap.of("bad-id", userInputInstance);
        EvaluationContext evaluationContext = session.getEvaluationContext().spawnWithCleanLifecycle(instances);

        XPathExpression xpe = XPathParseTool.parseXPath(
                "count(instance('my-search-input')/input/field[current()/@name = 'name'])");
        String result = FunctionUtils.toString(xpe.eval(evaluationContext));
        Assert.assertEquals("1", result);

        try {
            XPathExpression xpe1 = XPathParseTool.parseXPath(
                    "count(instance('bad-id')/input/field[current()/@name = 'name'])");
            FunctionUtils.toString(xpe1.eval(evaluationContext));
            Assert.fail("Expected exception");
        } catch (XPathMissingInstanceException e) {
            // this fails because we added this instance to the eval context with a different ID ('bad-id')
            Assert.assertTrue(e.getMessage().contains("search-input:patients"));
        }
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithExclude() throws Exception {
        testGetRawQueryParamsWithUserInputExcluded(
                ImmutableMap.of("exclude_patient_id", "123")
        );
    }

    private RemoteQuerySessionManager testGetRawQueryParamsWithUserInput(Map<String, String> userInput,
            List<String> expected, String key) throws Exception {
        return testGetRawQueryParamsWithUserInput(userInput, expected, key, null);
    }

    private RemoteQuerySessionManager testGetRawQueryParamsWithUserInput(Map<String, String> userInput,
            List<String> expected, String key, RemoteQuerySessionManager existingManager)
            throws Exception {
        RemoteQuerySessionManager remoteQuerySessionManager =
                existingManager == null ? buildRemoteQuerySessionManager() : existingManager;

        userInput.forEach(remoteQuerySessionManager::answerUserPrompt);

        Multimap<String, String> params = remoteQuerySessionManager.getRawQueryParams(true);

        Assert.assertEquals(expected, params.get(key));
        return remoteQuerySessionManager;
    }

    private void testGetRawQueryParamsWithUserInputExcluded(Map<String, String> userInput)
            throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        RemoteQuerySessionManager remoteQuerySessionManager = RemoteQuerySessionManager.buildQuerySessionManager(
                session, session.getEvaluationContext(), new ArrayList<>());

        userInput.forEach(remoteQuerySessionManager::answerUserPrompt);

        Multimap<String, String> params = remoteQuerySessionManager.getRawQueryParams(false);

        Assert.assertFalse(params.containsKey("exclude_patient_id"));
    }

    private ExternalDataInstance buildStateInstance() {
        Map<String, String> noAttrs = Collections.emptyMap();
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.parentNode("state", noAttrs, ImmutableList.of(
                        SimpleNode.textNode("id", noAttrs, "ka"),
                        SimpleNode.textNode("name", noAttrs, "Karnataka")
                )),
                SimpleNode.parentNode("state", noAttrs, ImmutableList.of(
                        SimpleNode.textNode("id", noAttrs, "rj"),
                        SimpleNode.textNode("name", noAttrs, "Rajasthan")
                ))
        );

        TreeElement root = TreeBuilder.buildTree("state", "state_list", nodes);
        return new ExternalDataInstance(ExternalDataInstance.JR_SEARCH_INPUT_REFERENCE, "state", root);
    }

    private ExternalDataInstance buildDistrictInstance() {
        Map<String, String> noAttrs = Collections.emptyMap();
        List<SimpleNode> nodes = ImmutableList.of(
                SimpleNode.parentNode("district", noAttrs, ImmutableList.of(
                        SimpleNode.textNode("id", noAttrs, "bang"),
                        SimpleNode.textNode("state_id", noAttrs, "ka"),
                        SimpleNode.textNode("name", noAttrs, "Bangalore")
                )),
                SimpleNode.parentNode("district", noAttrs, ImmutableList.of(
                        SimpleNode.textNode("id", noAttrs, "kota"),
                        SimpleNode.textNode("state_id", noAttrs, "rj"),
                        SimpleNode.textNode("name", noAttrs, "Kota")
                ))
        );

        TreeElement root = TreeBuilder.buildTree("district", "district_list", nodes);
        return new ExternalDataInstance(ExternalDataInstance.JR_SEARCH_INPUT_REFERENCE, "district", root);
    }

    @Test
    public void testErrorsWithUserInput_noInput() throws Exception {
        testErrorsWithUserInput(
                ImmutableMap.of(),
                ImmutableMap.of("age", "age should be greater than 18",
                        "another_age", "another age should be greater than 18"),
                null
        );
    }

    @Test
    public void testErrorsWithUserInput_errorsClearWithValidInput() throws Exception {
        RemoteQuerySessionManager remoteQuerySessionManager = testErrorsWithUserInput(
                ImmutableMap.of("name", "", "age", "15", "another_age", "12"),
                ImmutableMap.of("name", "name can't be empty", "age", "age should be greater than 18",
                        "another_age", "another age should be greater than 18"),
                null
        );

        testErrorsWithUserInput(
                ImmutableMap.of("name", "Ruth", "age", "21", "another_age", "20"),
                ImmutableMap.of(), remoteQuerySessionManager
        );
    }

    private RemoteQuerySessionManager testErrorsWithUserInput(Map<String, String> userInput,
            Map<String, String> expectedErrors, @Nullable RemoteQuerySessionManager existingManager)
            throws Exception {
        RemoteQuerySessionManager remoteQuerySessionManager =
                existingManager == null ? buildRemoteQuerySessionManager() : existingManager;

        userInput.forEach(remoteQuerySessionManager::answerUserPrompt);
        remoteQuerySessionManager.refresh();
        Hashtable<String, String> errors = remoteQuerySessionManager.getErrors();

        if(expectedErrors.isEmpty()){
           Assert.assertTrue(errors.isEmpty());
        }

        expectedErrors.forEach((key, expectedError) -> {
            Assert.assertEquals(expectedError, errors.get(key));
        });

        return remoteQuerySessionManager;
    }
}

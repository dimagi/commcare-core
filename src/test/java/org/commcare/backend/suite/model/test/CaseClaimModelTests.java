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
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        testPopulateItemsetChoices(ImmutableMap.of("state", "ka"), ImmutableList.of("bang"));
    }

    @Test
    public void testPopulateItemsetChoices__emptyInput() throws Exception {
        testPopulateItemsetChoices(Collections.emptyMap(), Collections.emptyList());
    }

    private void testPopulateItemsetChoices(Map<String, String> userInput, List<String> expected)
            throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        ExternalDataInstance districtInstance = buildDistrictInstance();
        EvaluationContext context = session.getEvaluationContext().spawnWithCleanLifecycle(ImmutableMap.of(
                districtInstance.getInstanceId(), districtInstance
        ));

        RemoteQuerySessionManager remoteQuerySessionManager = RemoteQuerySessionManager.buildQuerySessionManager(
                session, context, ImmutableList.of(QueryPrompt.INPUT_TYPE_SELECT1));

        userInput.forEach(remoteQuerySessionManager::answerUserPrompt);

        OrderedHashtable<String, QueryPrompt> inputDisplays =
                remoteQuerySessionManager.getNeededUserInputDisplays();
        QueryPrompt districtPrompt = inputDisplays.get("district");

        remoteQuerySessionManager.populateItemSetChoices(districtPrompt);
        List<String> choices = districtPrompt.getItemsetBinding().getChoices().stream().map(
                SelectChoice::getValue).collect(Collectors.toList());
        Assert.assertEquals(expected, choices);
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput() throws Exception {
        testGetRawQueryParamsWithUserInput(
                ImmutableMap.of("patient_id", "123"),
                ImmutableList.of("external_id = 123"),
                "patient_id"
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
    public void testRemoteRequestSessionManager_getRawQueryParamsWithUserInput_customInstanceId() throws Exception {
        testGetRawQueryParamsWithUserInput(
                ImmutableMap.of("patient_id", "123"),
                ImmutableList.of("external_id = 123"),
                "patient_id_custom_id"
        );
    }

    @Test
    public void testRemoteRequestSessionManager_getRawQueryParamsWithExclude() throws Exception {
        testGetRawQueryParamsWithUserInputExcluded(
                ImmutableMap.of("exclude_patient_id", "123")
        );
    }

    private void testGetRawQueryParamsWithUserInput(Map<String, String> userInput, List<String> expected, String key)
            throws Exception {
        MockApp mApp = new MockApp("/case_claim_example/");

        SessionWrapper session = mApp.getSession();
        session.setCommand("patient-search");

        RemoteQuerySessionManager remoteQuerySessionManager = RemoteQuerySessionManager.buildQuerySessionManager(
                session, session.getEvaluationContext(), new ArrayList<>());

        userInput.forEach(remoteQuerySessionManager::answerUserPrompt);

        Multimap<String, String> params = remoteQuerySessionManager.getRawQueryParams(true);

        Assert.assertEquals(expected, params.get(key));
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
}

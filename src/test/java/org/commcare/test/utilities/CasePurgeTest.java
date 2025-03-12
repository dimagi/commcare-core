package org.commcare.test.utilities;

import static org.commcare.cases.util.CasePurgeFilter.getFullCaseGraph;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.cases.util.InvalidCaseGraphException;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.DAG;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * A class for running tests on the case purge logic.
 *
 * Reads external JSON documents containing preconditions setting up case logic, and then
 * validates that the resulting case database is consistent with the purge/sync logic.
 *
 * Created by ctsims on 10/13/2015.
 */
@RunWith(Parameterized.class)
public class CasePurgeTest {

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> testData() {
        try {
            JSONArray fullTestResource =
                    new JSONArray(TestHelpers.getResourceAsString("/case_relationship_tests.json"));
            List<Object[]> listOfParameterSets = new ArrayList<>();
            for (int i = 0; i < fullTestResource.length(); ++i) {
                JSONObject root = fullTestResource.getJSONObject(i);
                listOfParameterSets.add(parseParametersFromJSONObject(root));
            }
            return listOfParameterSets;
        } catch (IOException | JSONException e) {
            RuntimeException failure =
                    new RuntimeException("Failed to parse input for CasePurgeTest");
            failure.initCause(e);
            throw failure;
        }
    }

    private static Object[] parseParametersFromJSONObject(JSONObject root) {
        Object[] parameters = new Object[8];
        parameters[0] = root.getString("name");

        String[] jsonArrayKeys =
                new String[]{"cases", "owned", "closed", "subcases", "extensions", "outcome", "relation_outcome"};
        for (int i = 0; i < jsonArrayKeys.length; i++) {
            addJSONArrayIfPresent(root, i+1, jsonArrayKeys[i], parameters);
        }

        return parameters;
    }

    private static void addJSONArrayIfPresent(JSONObject root, int index, String key,
                                              Object[] parameterSet) {
        if (root.has(key)) {
            parameterSet[index] = root.getJSONArray(key);
        }
    }

    private final String name;
    private final HashSet<String> cases = new HashSet<>();
    private final HashSet<String> ownedCases = new HashSet<>();
    private final HashSet<String> closedCases = new HashSet<>();
    private final HashSet<String> outcomeSet = new HashSet<>();
    private final HashMap<String, HashSet<String>> relationOutcomeSet = new HashMap<>();
    private final ArrayList<String[]> indices = new ArrayList<>();

    public CasePurgeTest(String name, JSONArray cases, JSONArray owned, JSONArray closed,
                         JSONArray subcases, JSONArray extensions, JSONArray outcome, JSONArray relationOutcomes) {
        this.name = name;
        createTestObjectsFromParameters(cases, owned, closed, subcases, extensions, outcome, relationOutcomes);
    }

    private void createTestObjectsFromParameters(JSONArray casesJson, JSONArray ownedJson,
            JSONArray closedJson, JSONArray subcasesJson,
            JSONArray extensionsJson, JSONArray outcomeJson,
            JSONArray relationOutcomes) {
        if (casesJson != null) {
            getCases(casesJson, cases);
        }
        if (ownedJson != null) {
            getCases(ownedJson, ownedCases);
        }
        if (closedJson != null) {
            getCases(closedJson, closedCases);
        }

        if (subcasesJson != null) {
            getIndices(subcasesJson, indices, CaseIndex.RELATIONSHIP_CHILD);
        }
        if (extensionsJson != null) {
            getIndices(extensionsJson, indices, CaseIndex.RELATIONSHIP_EXTENSION);
        }
        getCases(outcomeJson, outcomeSet);
        if (relationOutcomes != null) {
            populateRelationOutcomes(relationOutcomes, outcomeSet);
        }
    }

    private void populateRelationOutcomes(JSONArray relationOutcomes, HashSet<String> outcomeSet)
            throws JSONException {
        int count = 0;
        for (String outcome : outcomeSet) {
            JSONObject relationOutcome = (JSONObject)relationOutcomes.get(count++);
            JSONArray relatedCases = relationOutcome.optJSONArray("related_cases");
            HashSet<String> relatedCasesSet = new HashSet<>();
            for (int i = 0; i < relatedCases.length(); ++i) {
                relatedCasesSet.add(relatedCases.getString(i));
            }
            relationOutcomeSet.put(outcome, relatedCasesSet);
        }
    }

    private void getIndices(JSONArray indices, ArrayList<String[]> indexSet,
                            String indexType) throws JSONException {
        for (int i = 0; i < indices.length(); ++i) {
            JSONArray index = indices.getJSONArray(i);
            String c = index.getString(0);
            String target = index.getString(1);
            cases.add(c);
            cases.add(target);
            indexSet.add(new String[]{c, target, indexType});
        }
    }

    private void getCases(JSONArray owned, HashSet<String> target) throws JSONException {
        for (int i = 0; i < owned.length(); ++i) {
            String c = owned.getString(i);
            cases.add(c);
            target.add(c);
        }
    }

    @Test
    public void executeTest() throws InvalidCaseGraphException {
        DummyIndexedStorageUtility<Case> storage =
                new DummyIndexedStorageUtility<>(Case.class, new LivePrototypeFactory());

        String userId = "user";

        initCaseStorage(storage, userId);

        Vector<String> ownerIds = new Vector<>();
        ownerIds.add(userId);


        storage.removeAll(new CasePurgeFilter(getFullCaseGraph(storage, ownerIds)));

        HashSet<String> inStorage = new HashSet<>();
        // redo the graph as we don't want the eliminated cases anymore
        DAG<String, int[], String> graph = getFullCaseGraph(storage, ownerIds);
        for (IStorageIterator<Case> iterator = storage.iterate(); iterator.hasMore(); ) {
            Case c = iterator.nextRecord();
            String caseId = c.getCaseId();
            inStorage.add(caseId);


            HashSet<String> relatedCasesSet = relationOutcomeSet.get(caseId);
            HashSet<String> input = new HashSet<>();
            input.add(caseId);
            Set<String> relatedCases = graph.findConnectedRecords(input);
            Assert.assertEquals(name, relatedCasesSet, relatedCases);
        }

        Assert.assertEquals(name, outcomeSet, inStorage);
    }

    private void initCaseStorage(DummyIndexedStorageUtility<Case> storage,
                                 String userId) {
        for (String c : cases) {
            Case theCase = new Case(c, "purge_test_case");
            theCase.setCaseId(c);
            if (ownedCases.contains(c)) {
                theCase.setUserId(userId);
            }
            if (closedCases.contains(c)) {
                theCase.setClosed(true);
            }
            storage.write(theCase);
        }

        for (String[] index : indices) {
            Case theCase = storage.getRecordForValue(Case.INDEX_CASE_ID, index[0]);
            CaseIndex caseIndex =
                    new CaseIndex(index[0] + index[1] + index[2],
                            "purge_test_case", index[1], index[2]);
            theCase.setIndex(caseIndex);
            storage.write(theCase);
        }
    }

    public String getName() {
        return name;
    }
    
}

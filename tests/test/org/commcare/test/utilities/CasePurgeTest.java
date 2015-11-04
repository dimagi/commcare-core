package org.commcare.test.utilities;

import org.commcare.cases.model.Case;
import org.commcare.cases.model.CaseIndex;
import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.util.mocks.LivePrototypeFactory;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

/**
 * A class for running tests on the case purge logic.
 *
 * Reads external JSON documents containing preconditions setting up case logic, and then
 * validates that the resulting case database is consistent with the purge/sync logic.
 *
 * Created by ctsims on 10/13/2015.
 */
public class CasePurgeTest {

    private final String name;

    private final HashSet<String> cases = new HashSet<>();

    private final HashSet<String> ownedCases = new HashSet<>();
    private final HashSet<String> closedCases = new HashSet<>();
    private final HashSet<String> outcome = new HashSet<>();

    private final ArrayList<String[]> indices = new ArrayList<>();

    public static ArrayList<CasePurgeTest> getTests(String resourceName) {
        try {
            ArrayList<CasePurgeTest> runners = new ArrayList<>();
            JSONArray tests = new JSONArray(TestHelpers.getResourceAsString(resourceName));
            for (int i = 0; i < tests.length(); ++i) {
                JSONObject root = tests.getJSONObject(i);
                runners.add(new CasePurgeTest(root));
            }
            return runners;
        } catch (IOException | JSONException e) {
            RuntimeException failure =
                    new RuntimeException("Failed to parse input for test: " + resourceName);
            failure.initCause(e);
            throw failure;
        }
    }

    private CasePurgeTest(JSONObject root) throws JSONException {
        name = root.getString("name");
        if (root.has("cases")) {
            getCases(root.getJSONArray("cases"), cases);
        }
        if (root.has("owned")) {
            getCases(root.getJSONArray("owned"), ownedCases);
        }
        if (root.has("closed")) {
            getCases(root.getJSONArray("closed"), closedCases);
        }

        if (root.has("subcases")) {
            getIndices(root.getJSONArray("subcases"), indices, CaseIndex.RELATIONSHIP_CHILD);
        }
        if (root.has("extensions")) {
            getIndices(root.getJSONArray("extensions"), indices, CaseIndex.RELATIONSHIP_EXTENSION);
        }
        getCases(root.getJSONArray("outcome"), outcome);
    }

    private void getIndices(JSONArray indices,
                            ArrayList<String[]> indexSet,
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

    public void executeTest() {
        DummyIndexedStorageUtility<Case> storage =
                new DummyIndexedStorageUtility<>(Case.class, new LivePrototypeFactory());

        String userId = "user";

        initCaseStorage(storage, userId);

        Vector<String> ownerIds = new Vector<>();
        ownerIds.add(userId);

        storage.removeAll(new CasePurgeFilter(storage, ownerIds));

        HashSet<String> inStorage = new HashSet<>();
        for (IStorageIterator<Case> iterator = storage.iterate(); iterator.hasMore(); ) {
            Case c = iterator.nextRecord();
            inStorage.add(c.getCaseId());
        }

        Assert.assertEquals(name, outcome, inStorage);

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
